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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ChatService {

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
