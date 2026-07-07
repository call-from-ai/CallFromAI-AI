package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.context.ContextLoader;
import com.example.aidatingagentbackend.context.ContextUpdater;
import com.example.aidatingagentbackend.dto.ChatRequest;
import com.example.aidatingagentbackend.dto.ChatResponse;
import com.example.aidatingagentbackend.dto.Context;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.entity.ChatMessage;
import com.example.aidatingagentbackend.entity.ResponseQualityEvaluation;
import com.example.aidatingagentbackend.prompt.PromptBuilder;
import com.example.aidatingagentbackend.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final long STREAM_TIMEOUT_MS = 120_000L;

    private final PromptBuilder promptBuilder;
    private final GeminiService geminiService;

    private final ContextLoader contextLoader;
    private final ChatMessageRepository chatMessageRepository;
    private final ContextUpdater contextUpdater;
    private final EmotionUpdateService emotionUpdateService;
    private final ResponseQualityEvaluatorService responseQualityEvaluatorService;
    private final AgentWorldStateService agentWorldStateService;
    private final AgentGoalService agentGoalService;

    public ChatService(
            PromptBuilder promptBuilder,
            GeminiService geminiService,
            ContextLoader contextLoader,
            ChatMessageRepository chatMessageRepository,
            ContextUpdater contextUpdater,
            EmotionUpdateService emotionUpdateService,
            ResponseQualityEvaluatorService responseQualityEvaluatorService,
            AgentWorldStateService agentWorldStateService,
            AgentGoalService agentGoalService) {
        this.promptBuilder = promptBuilder;
        this.geminiService = geminiService;
        this.contextLoader = contextLoader;
        this.chatMessageRepository = chatMessageRepository;
        this.contextUpdater = contextUpdater;
        this.emotionUpdateService = emotionUpdateService;
        this.responseQualityEvaluatorService = responseQualityEvaluatorService;
        this.agentWorldStateService = agentWorldStateService;
        this.agentGoalService = agentGoalService;
    }

    public ChatResponse chat(ChatRequest request){

        EmotionUpdateService.EmotionUpdateResult emotionUpdateResult =
                emotionUpdateService.updateBeforeResponse(request.getUserId(), request.getMessage());
        EventAnalysis eventAnalysis = emotionUpdateResult.eventAnalysis();
        contextUpdater.updateBeforeResponse(request.getMessage(), eventAnalysis);
        agentWorldStateService.updateBeforeResponse(request.getUserId());
        agentGoalService.selectCurrentGoal(request.getUserId());

        Context context =
                contextLoader.load(request.getUserId(), request.getMessage());

        String prompt =

                promptBuilder.builder()

                        .character(context.character())

                        .state(context.state())

                        .relationship(context.relationship())

                        .agentSelfState(context.agentSelfState())

                        .agentProfile(context.agentProfile())

                        .agentWorldState(context.agentWorldState())

                        .agentGoal(context.agentGoal())

                        .agentInitiative(context.agentInitiative())

                        .characterExamples(context.characterExamples())

                        .memories(context.memories())

                        .reflections(context.reflections())

                        .turningPoints(context.turningPoints())

                        .chatHistory(context.history())

                        .userMessage(request.getMessage())

                        .build();
        String reply =
                geminiService.generate(prompt);

        reply = evaluateAndRegenerateIfNeeded(request, context, prompt, reply, eventAnalysis);

        save(request.getUserId(),"USER",request.getMessage());
        save(request.getUserId(),"ASSISTANT",reply);

        contextUpdater.updateMemoryAfterResponse(request.getMessage(),reply);

        return new ChatResponse(reply);
    }

    public SseEmitter sendMessageStream(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);

        CompletableFuture.runAsync(() -> {
            long startedAt = System.currentTimeMillis();
            long firstTokenAt = -1L;
            AtomicBoolean firstChunkSent = new AtomicBoolean(false);
            StringBuilder streamedReply = new StringBuilder();

            geminiService.resetCallCount();
            try {
                EmotionUpdateService.EmotionUpdateResult emotionUpdateResult =
                        emotionUpdateService.updateBeforeResponse(request.getUserId(), request.getMessage());
                EventAnalysis eventAnalysis = emotionUpdateResult.eventAnalysis();
                contextUpdater.updateBeforeResponse(request.getMessage(), eventAnalysis);
                agentWorldStateService.updateBeforeResponse(request.getUserId());
                agentGoalService.selectCurrentGoal(request.getUserId());

                Context context =
                        contextLoader.load(request.getUserId(), request.getMessage());

                String prompt = buildPrompt(context, request.getMessage(), true);

                sendEvent(emitter, "meta", Map.of(
                        "eventType", eventAnalysis.eventType().name(),
                        "compactPrompt", true
                ));

                final long[] firstTokenHolder = {firstTokenAt};
                geminiService.generateStream(prompt, chunk -> {
                    if (firstChunkSent.compareAndSet(false, true)) {
                        firstTokenHolder[0] = System.currentTimeMillis();
                    }
                    streamedReply.append(chunk);
                    sendEvent(emitter, "chunk", Map.of("text", chunk));
                });
                firstTokenAt = firstTokenHolder[0];

                String reply = streamedReply.toString();
                save(request.getUserId(), "USER", request.getMessage());
                save(request.getUserId(), "ASSISTANT", reply);
                contextUpdater.updateMemoryAfterResponse(request.getMessage(), reply);

                if (responseQualityEvaluatorService.shouldEvaluate(eventAnalysis, context)) {
                    responseQualityEvaluatorService.evaluateAndSave(
                            request.getUserId(),
                            request.getMessage(),
                            reply,
                            context,
                            eventAnalysis,
                            false
                    );
                }

                long completedAt = System.currentTimeMillis();
                long firstTokenLatencyMs = firstTokenAt < 0 ? completedAt - startedAt : firstTokenAt - startedAt;
                long totalLatencyMs = completedAt - startedAt;
                int llmCallCount = geminiService.currentCallCount();
                log.info(
                        "chat.stream latency userId={} firstTokenLatencyMs={} totalLatencyMs={} llmCallCount={}",
                        request.getUserId(),
                        firstTokenLatencyMs,
                        totalLatencyMs,
                        llmCallCount
                );
                sendEvent(emitter, "done", Map.of(
                        "firstTokenLatencyMs", firstTokenLatencyMs,
                        "totalLatencyMs", totalLatencyMs,
                        "llmCallCount", llmCallCount
                ));
                emitter.complete();
            } catch (Exception exception) {
                log.warn("chat.stream failed userId={}", request.getUserId(), exception);
                sendEvent(emitter, "error", Map.of("message", exception.getMessage() == null ? "stream failed" : exception.getMessage()));
                emitter.completeWithError(exception);
            } finally {
                geminiService.clearCallCount();
            }
        });

        return emitter;
    }

    private String evaluateAndRegenerateIfNeeded(
            ChatRequest request,
            Context context,
            String prompt,
            String reply,
            EventAnalysis eventAnalysis
    ) {
        if (!responseQualityEvaluatorService.shouldEvaluate(eventAnalysis, context)) {
            return reply;
        }

        ResponseQualityEvaluation evaluation =
                responseQualityEvaluatorService.evaluateAndSave(
                        request.getUserId(),
                        request.getMessage(),
                        reply,
                        context,
                        eventAnalysis,
                        false
                );

        if (!responseQualityEvaluatorService.shouldRegenerate(evaluation)) {
            return reply;
        }

        String regenerationPrompt =
                promptBuilder.buildRegenerationPrompt(prompt, reply, evaluation);
        String regeneratedReply = geminiService.generate(regenerationPrompt);
        responseQualityEvaluatorService.evaluateAndSave(
                request.getUserId(),
                request.getMessage(),
                regeneratedReply,
                context,
                eventAnalysis,
                true
        );
        return regeneratedReply;
    }

    private String buildPrompt(Context context, String userMessage, boolean compactMode) {
        return promptBuilder.builder()
                .character(context.character())
                .state(context.state())
                .relationship(context.relationship())
                .agentSelfState(context.agentSelfState())
                .agentProfile(context.agentProfile())
                .agentWorldState(context.agentWorldState())
                .agentGoal(context.agentGoal())
                .agentInitiative(context.agentInitiative())
                .characterExamples(context.characterExamples())
                .memories(context.memories())
                .reflections(context.reflections())
                .turningPoints(context.turningPoints())
                .chatHistory(context.history())
                .userMessage(userMessage)
                .compactMode(compactMode)
                .build();
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to send stream event.", exception);
        }
    }

    private void save(Long characterId,
                      String role,
                      String content){

        ChatMessage message=new ChatMessage();

        message.setCharacterId(characterId);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());

        chatMessageRepository.save(message);
    }
}
