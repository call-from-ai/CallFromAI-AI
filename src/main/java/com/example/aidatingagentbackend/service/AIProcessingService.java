package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.context.ContextLoader;
import com.example.aidatingagentbackend.context.ContextUpdater;
import com.example.aidatingagentbackend.dto.ChatRequest;
import com.example.aidatingagentbackend.dto.Context;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.entity.ChatMessage;
import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import com.example.aidatingagentbackend.entity.ResponseQualityEvaluation;
import com.example.aidatingagentbackend.prompt.PromptBuilder;
import com.example.aidatingagentbackend.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AIProcessingService {

    private final PromptBuilder promptBuilder;
    private final GeminiService geminiService;
    private final ContextLoader contextLoader;
    private final ChatMessageRepository chatMessageRepository;
    private final ContextUpdater contextUpdater;
    private final EmotionUpdateService emotionUpdateService;
    private final ResponseQualityEvaluatorService responseQualityEvaluatorService;
    private final AgentWorldStateService agentWorldStateService;
    private final AgentGoalService agentGoalService;
    private final ResponseStylePostProcessor responseStylePostProcessor;
    private final ConversationEventService conversationEventService;
    private final CharacterPreferenceService characterPreferenceService;

    public AIProcessingService(
            PromptBuilder promptBuilder,
            GeminiService geminiService,
            ContextLoader contextLoader,
            ChatMessageRepository chatMessageRepository,
            ContextUpdater contextUpdater,
            EmotionUpdateService emotionUpdateService,
            ResponseQualityEvaluatorService responseQualityEvaluatorService,
            AgentWorldStateService agentWorldStateService,
            AgentGoalService agentGoalService,
            ResponseStylePostProcessor responseStylePostProcessor,
            ConversationEventService conversationEventService,
            CharacterPreferenceService characterPreferenceService
    ) {
        this.promptBuilder = promptBuilder;
        this.geminiService = geminiService;
        this.contextLoader = contextLoader;
        this.chatMessageRepository = chatMessageRepository;
        this.contextUpdater = contextUpdater;
        this.emotionUpdateService = emotionUpdateService;
        this.responseQualityEvaluatorService = responseQualityEvaluatorService;
        this.agentWorldStateService = agentWorldStateService;
        this.agentGoalService = agentGoalService;
        this.responseStylePostProcessor = responseStylePostProcessor;
        this.conversationEventService = conversationEventService;
        this.characterPreferenceService = characterPreferenceService;
    }

    public PreparedAIProcessing prepare(ChatRequest request, boolean compactPrompt) {
        Long characterId = resolveCharacterId(request);
        String userMessage = request.getMessage();

        EmotionUpdateService.EmotionUpdateResult emotionUpdateResult =
                emotionUpdateService.updateBeforeResponse(characterId, userMessage);
        EventAnalysis eventAnalysis = emotionUpdateResult.eventAnalysis();
        RelationshipTemperature relationshipTemperature = resolveRelationshipTemperature(request);

        conversationEventService.detectAndSave(characterId, userMessage, eventAnalysis);
        contextUpdater.updateBeforeResponse(characterId, userMessage, eventAnalysis);
        agentWorldStateService.updateBeforeResponse(characterId);
        agentGoalService.selectCurrentGoal(characterId);

        Context context = contextLoader.load(
                characterId,
                userMessage,
                eventAnalysis,
                relationshipTemperature
        );
        String prompt = buildPrompt(context, userMessage, compactPrompt);

        return new PreparedAIProcessing(
                characterId,
                userMessage,
                eventAnalysis,
                context,
                prompt
        );
    }

    public CompletedAIProcessing process(ChatRequest request) {
        PreparedAIProcessing prepared = prepare(request, false);
        String reply = geminiService.generate(prepared.prompt());
        reply = finishGeneratedReply(prepared, reply, true);
        return new CompletedAIProcessing(prepared, reply);
    }

    public String finishGeneratedReply(
            PreparedAIProcessing prepared,
            String generatedReply,
            boolean allowRegeneration
    ) {
        String reply = postProcess(prepared.context(), generatedReply);
        if (allowRegeneration) {
            reply = evaluateAndRegenerateIfNeeded(prepared, reply);
        } else {
            evaluateIfNeeded(prepared, reply);
        }
        persistAfterResponse(prepared, reply);
        return reply;
    }

    public String buildPrompt(Context context, String userMessage, boolean compactMode) {
        return promptBuilder.builder()
                .character(context.character())
                .state(context.state())
                .relationship(context.relationship())
                .characterTraitProfile(context.characterTraitProfile())
                .relationshipStage(context.relationshipStage())
                .relationshipTemperatureScore(context.relationshipTemperatureScore())
                .romanceStyleScore(context.romanceStyleScore())
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
                .chatHistory(context.history())
                .userMessage(userMessage)
                .compactMode(compactMode)
                .build();
    }

    private String evaluateAndRegenerateIfNeeded(PreparedAIProcessing prepared, String reply) {
        Context context = prepared.context();
        EventAnalysis eventAnalysis = prepared.eventAnalysis();
        if (!responseQualityEvaluatorService.shouldEvaluate(eventAnalysis, context)) {
            return reply;
        }

        ResponseQualityEvaluation evaluation =
                responseQualityEvaluatorService.evaluateAndSave(
                        prepared.characterId(),
                        prepared.userMessage(),
                        reply,
                        context,
                        eventAnalysis,
                        false
                );

        if (!responseQualityEvaluatorService.shouldRegenerate(evaluation)) {
            return reply;
        }

        String regenerationPrompt = promptBuilder.buildRegenerationPrompt(
                prepared.prompt(),
                reply,
                evaluation
        );
        String regeneratedReply = geminiService.generate(regenerationPrompt);
        regeneratedReply = postProcess(context, regeneratedReply);
        responseQualityEvaluatorService.evaluateAndSave(
                prepared.characterId(),
                prepared.userMessage(),
                regeneratedReply,
                context,
                eventAnalysis,
                true
        );
        return regeneratedReply;
    }

    private void evaluateIfNeeded(PreparedAIProcessing prepared, String reply) {
        if (!responseQualityEvaluatorService.shouldEvaluate(prepared.eventAnalysis(), prepared.context())) {
            return;
        }
        responseQualityEvaluatorService.evaluateAndSave(
                prepared.characterId(),
                prepared.userMessage(),
                reply,
                prepared.context(),
                prepared.eventAnalysis(),
                false
        );
    }

    private String postProcess(Context context, String reply) {
        return responseStylePostProcessor.process(
                reply,
                context.relationshipTemperature(),
                context.relationshipTemperatureScore(),
                context.romanceStyleScore(),
                context.characterTraitProfile(),
                context.relationshipStage(),
                context.agentSelfState()
        );
    }

    private void persistAfterResponse(PreparedAIProcessing prepared, String reply) {
        save(prepared.characterId(), "USER", prepared.userMessage());
        save(prepared.characterId(), "ASSISTANT", reply);
        characterPreferenceService.persistInventedPreferenceIfNeeded(
                prepared.characterId(),
                prepared.userMessage(),
                reply,
                prepared.context().preferenceQuestionPlan()
        );
        contextUpdater.updateMemoryAfterResponse(prepared.characterId(), prepared.userMessage(), reply);
    }

    private void save(Long characterId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setCharacterId(characterId);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        chatMessageRepository.save(message);
    }

    private RelationshipTemperature resolveRelationshipTemperature(ChatRequest request) {
        return request.getRelationshipTemperature() == null
                ? RelationshipTemperature.NEUTRAL
                : request.getRelationshipTemperature();
    }

    public Long resolveCharacterId(ChatRequest request) {
        Long characterId = request.resolveCharacterId();
        if (characterId == null) {
            throw new IllegalArgumentException("characterId is required. userId is still accepted as a legacy alias.");
        }
        return characterId;
    }

    public record PreparedAIProcessing(
            Long characterId,
            String userMessage,
            EventAnalysis eventAnalysis,
            Context context,
            String prompt
    ) {
    }

    public record CompletedAIProcessing(
            PreparedAIProcessing prepared,
            String reply
    ) {
    }
}
