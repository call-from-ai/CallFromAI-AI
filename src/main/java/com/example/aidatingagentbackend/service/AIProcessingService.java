package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.context.ContextLoader;
import com.example.aidatingagentbackend.context.ContextUpdater;
import com.example.aidatingagentbackend.dto.ChatRequest;
import com.example.aidatingagentbackend.dto.Context;
import com.example.aidatingagentbackend.dto.GeminiImage;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.entity.ResponseQualityEvaluation;
import com.example.aidatingagentbackend.prompt.PromptBuilder;
import org.springframework.stereotype.Service;

@Service
public class AIProcessingService {

    private final PromptBuilder promptBuilder;
    private final GeminiService geminiService;
    private final ContextLoader contextLoader;
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
        validateSnapshots(request);
        String userMessage = request.getMessage();

        EmotionUpdateService.EmotionUpdateResult emotionUpdateResult =
                emotionUpdateService.updateBeforeResponse(characterId, userMessage, request.getHistory(),
                        request.getCharacter().traits(), request.getRelationship());
        EventAnalysis eventAnalysis = emotionUpdateResult.eventAnalysis();

        conversationEventService.detectAndSave(characterId, userMessage, eventAnalysis);
        ContextUpdater.RelationshipUpdate relationshipUpdate = contextUpdater.updateBeforeResponse(
                characterId, request.getRelationship(), userMessage, eventAnalysis);
        agentWorldStateService.updateBeforeResponse(characterId, request.getCharacter(), relationshipUpdate.nextRelationship());
        agentGoalService.selectCurrentGoal(characterId, relationshipUpdate.nextRelationship());

        Context context = contextLoader.load(request, eventAnalysis, relationshipUpdate);
        String prompt = buildPrompt(context, userMessage, compactPrompt);

        return new PreparedAIProcessing(
                characterId,
                userMessage,
                eventAnalysis,
                emotionUpdateResult,
                relationshipUpdate,
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

    public CompletedAIProcessing process(ChatRequest request, GeminiImage image) {
        PreparedAIProcessing prepared = prepare(request, false);
        String reply = geminiService.generate(prepared.prompt(), image);
        reply = finishGeneratedReply(prepared, reply, true, image);
        return new CompletedAIProcessing(prepared, reply);
    }

    public String finishGeneratedReply(
            PreparedAIProcessing prepared,
            String generatedReply,
            boolean allowRegeneration
    ) {
        return finishGeneratedReply(prepared, generatedReply, allowRegeneration, null);
    }

    public String finishGeneratedReply(
            PreparedAIProcessing prepared,
            String generatedReply,
            boolean allowRegeneration,
            GeminiImage image
    ) {
        String reply = postProcess(prepared.context(), generatedReply);
        if (allowRegeneration) {
            reply = evaluateAndRegenerateIfNeeded(prepared, reply, image);
        } else {
            evaluateIfNeeded(prepared, reply);
        }
        persistAfterResponse(prepared, reply);
        return reply;
    }

    public String buildPrompt(Context context, String userMessage, boolean compactMode) {
        return promptBuilder.builder()
                .character(context.character())
                .relationship(context.relationship())
                .characterTraitProfile(context.characterTraitProfile())
                .relationshipStage(context.relationshipStage())
                .relationshipTemperatureScore(context.relationshipTemperatureScore())
                .romanceStyleScore(context.romanceStyleScore())
                .agentSelfState(context.agentSelfState())
                .agentWorldState(context.agentWorldState())
                .agentGoal(context.agentGoal())
                .agentInitiative(context.agentInitiative())
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

    private String evaluateAndRegenerateIfNeeded(
            PreparedAIProcessing prepared,
            String reply,
            GeminiImage image
    ) {
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
        String regeneratedReply = geminiService.generate(regenerationPrompt, image);
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
                context.relationshipStrategy(),
                context.relationshipTemperatureScore(),
                context.romanceStyleScore(),
                context.characterTraitProfile(),
                context.relationshipStage(),
                context.agentSelfState()
        );
    }

    private void persistAfterResponse(PreparedAIProcessing prepared, String reply) {
        characterPreferenceService.persistInventedPreferenceIfNeeded(
                prepared.characterId(),
                prepared.userMessage(),
                reply,
                prepared.context().preferenceQuestionPlan()
        );
        contextUpdater.updateMemoryAfterResponse(prepared.characterId(), prepared.userMessage(), reply);
    }

    public Long resolveCharacterId(ChatRequest request) {
        Long characterId = request.resolveCharacterId();
        if (characterId == null) {
            throw new IllegalArgumentException("character.characterId is required.");
        }
        return characterId;
    }

    private void validateSnapshots(ChatRequest request) {
        if (request == null || request.getCharacter() == null) throw new IllegalArgumentException("character snapshot is required");
        if (request.getRelationship() == null) throw new IllegalArgumentException("relationship snapshot is required");
        if (request.getCharacter().traits() == null) throw new IllegalArgumentException("character.traits is required");
    }

    public record PreparedAIProcessing(
            Long characterId,
            String userMessage,
            EventAnalysis eventAnalysis,
            EmotionUpdateService.EmotionUpdateResult emotionUpdateResult,
            ContextUpdater.RelationshipUpdate relationshipUpdate,
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
