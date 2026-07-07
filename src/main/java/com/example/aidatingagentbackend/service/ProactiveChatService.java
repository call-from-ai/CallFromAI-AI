package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.context.ContextLoader;
import com.example.aidatingagentbackend.dto.Context;
import com.example.aidatingagentbackend.entity.ChatMessage;
import com.example.aidatingagentbackend.prompt.PromptBuilder;
import com.example.aidatingagentbackend.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ProactiveChatService {

    private static final Logger log = LoggerFactory.getLogger(ProactiveChatService.class);
    private static final long NO_TIMEOUT = 0L;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();
    private final ContextLoader contextLoader;
    private final PromptBuilder promptBuilder;
    private final GeminiService geminiService;
    private final ChatMessageRepository chatMessageRepository;
    private final AgentWorldStateService agentWorldStateService;
    private final AgentGoalService agentGoalService;
    private final ResponseStylePostProcessor responseStylePostProcessor;

    public ProactiveChatService(
            ContextLoader contextLoader,
            PromptBuilder promptBuilder,
            GeminiService geminiService,
            ChatMessageRepository chatMessageRepository,
            AgentWorldStateService agentWorldStateService,
            AgentGoalService agentGoalService,
            ResponseStylePostProcessor responseStylePostProcessor
    ) {
        this.contextLoader = contextLoader;
        this.promptBuilder = promptBuilder;
        this.geminiService = geminiService;
        this.chatMessageRepository = chatMessageRepository;
        this.agentWorldStateService = agentWorldStateService;
        this.agentGoalService = agentGoalService;
        this.responseStylePostProcessor = responseStylePostProcessor;
    }

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(NO_TIMEOUT);
        emittersByUserId.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(exception -> removeEmitter(userId, emitter));

        sendEvent(userId, emitter, "connected", Map.of(
                "userId", userId,
                "message", "proactive chat stream connected"
        ));
        return emitter;
    }

    public void sendNow(Long userId) {
        sendProactiveMessage(userId);
    }

    @Scheduled(fixedRateString = "${agent.proactive.fixed-rate-ms:3600000}")
    public void sendHourlyProactiveMessages() {
        Set<Long> connectedUserIds = emittersByUserId.keySet();
        if (connectedUserIds.isEmpty()) {
            return;
        }

        for (Long userId : connectedUserIds) {
            if (hasNoActiveEmitter(userId)) {
                emittersByUserId.remove(userId);
                continue;
            }
            sendProactiveMessage(userId);
        }
    }

    @Scheduled(fixedRateString = "${agent.proactive.heartbeat-rate-ms:30000}")
    public void sendHeartbeat() {
        for (Long userId : new ArrayList<>(emittersByUserId.keySet())) {
            for (SseEmitter emitter : emittersByUserId.getOrDefault(userId, new CopyOnWriteArrayList<>())) {
                sendEvent(userId, emitter, "heartbeat", Map.of("time", LocalDateTime.now().toString()));
            }
        }
    }

    private void sendProactiveMessage(Long userId) {
        long startedAt = System.currentTimeMillis();
        geminiService.resetCallCount();
        try {
            agentWorldStateService.updateBeforeResponse(userId);
            agentGoalService.selectCurrentGoal(userId);

            String promptSeed = "The user has not sent a new message. Send one short proactive check-in as the agent. "
                    + "Do not pretend to perform real physical actions. Do not guilt-trip, pressure, or demand a reply.";
            Context context = contextLoader.load(userId, promptSeed);
            String prompt = buildPrompt(context, promptSeed);
            String reply = geminiService.generate(prompt);
            reply = responseStylePostProcessor.process(reply, context.relationshipTemperature());
            saveAssistantMessage(userId, reply);

            long totalLatencyMs = System.currentTimeMillis() - startedAt;
            int llmCallCount = geminiService.currentCallCount();
            broadcast(userId, "proactive", Map.of(
                    "text", reply,
                    "totalLatencyMs", totalLatencyMs,
                    "llmCallCount", llmCallCount
            ));
            log.info(
                    "chat.proactive sent userId={} totalLatencyMs={} llmCallCount={}",
                    userId,
                    totalLatencyMs,
                    llmCallCount
            );
        } catch (Exception exception) {
            log.warn("chat.proactive failed userId={}", userId, exception);
            broadcast(userId, "error", Map.of("message", "failed to generate proactive message"));
        } finally {
            geminiService.clearCallCount();
        }
    }

    private String buildPrompt(Context context, String promptSeed) {
        return promptBuilder.builder()
                .character(context.character())
                .state(context.state())
                .relationship(context.relationship())
                .agentSelfState(context.agentSelfState())
                .agentProfile(context.agentProfile())
                .agentWorldState(context.agentWorldState())
                .agentGoal(context.agentGoal())
                .agentInitiative(context.agentInitiative())
                .relationshipTemperature(context.relationshipTemperature())
                .agentLifeEvents(context.agentLifeEvents())
                .conversationEvents(context.conversationEvents())
                .preferenceQuestionPlan(context.preferenceQuestionPlan())
                .conversationTopicPlan(context.conversationTopicPlan())
                .characterPreferences(context.characterPreferences())
                .characterExamples(context.characterExamples())
                .memories(context.memories())
                .reflections(context.reflections())
                .turningPoints(context.turningPoints())
                .chatHistory(context.history())
                .userMessage(promptSeed)
                .compactMode(true)
                .build();
    }

    private void saveAssistantMessage(Long userId, String reply) {
        ChatMessage message = new ChatMessage();
        message.setCharacterId(userId);
        message.setRole("ASSISTANT");
        message.setContent(reply);
        message.setCreatedAt(LocalDateTime.now());
        chatMessageRepository.save(message);
    }

    private void broadcast(Long userId, String eventName, Object data) {
        for (SseEmitter emitter : emittersByUserId.getOrDefault(userId, new CopyOnWriteArrayList<>())) {
            sendEvent(userId, emitter, eventName, data);
        }
    }

    private void sendEvent(Long userId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException | IllegalStateException exception) {
            removeEmitter(userId, emitter);
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByUserId.remove(userId);
        }
    }

    private boolean hasNoActiveEmitter(Long userId) {
        return emittersByUserId.getOrDefault(userId, new CopyOnWriteArrayList<>()).isEmpty();
    }
}
