package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.context.ContextLoader;
import com.example.aidatingagentbackend.context.ContextUpdater;
import com.example.aidatingagentbackend.dto.ChatRequest;
import com.example.aidatingagentbackend.dto.ChatResponse;
import com.example.aidatingagentbackend.dto.Context;
import com.example.aidatingagentbackend.entity.ChatMessage;
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

    public ChatService(
            PromptBuilder promptBuilder,
            GeminiService geminiService,
            ContextLoader contextLoader,
            ChatMessageRepository chatMessageRepository,
            ContextUpdater contextUpdater,
            EmotionUpdateService emotionUpdateService) {
        this.promptBuilder = promptBuilder;
        this.geminiService = geminiService;
        this.contextLoader = contextLoader;
        this.chatMessageRepository = chatMessageRepository;
        this.contextUpdater = contextUpdater;
        this.emotionUpdateService = emotionUpdateService;
    }

    public ChatResponse chat(ChatRequest request){

        emotionUpdateService.updateBeforeResponse(request.getUserId(), request.getMessage());
        contextUpdater.updateBeforeResponse(request.getMessage());

        Context context =
                contextLoader.load(request.getUserId(), request.getMessage());

        String prompt =

                promptBuilder.builder()

                        .character(context.character())

                        .state(context.state())

                        .relationship(context.relationship())

                        .agentSelfState(context.agentSelfState())

                        .memories(context.memories())

                        .turningPoints(context.turningPoints())

                        .chatHistory(context.history())

                        .userMessage(request.getMessage())

                        .build();
        String reply =
                geminiService.generate(prompt);

        save(request.getUserId(),"USER",request.getMessage());
        save(request.getUserId(),"ASSISTANT",reply);

        contextUpdater.updateMemoryAfterResponse(request.getMessage(),reply);

        return new ChatResponse(reply);
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
