package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.ChatRequest;
import com.example.aidatingagentbackend.dto.ChatResponse;
import com.example.aidatingagentbackend.engine.EmotionEngine;
import com.example.aidatingagentbackend.engine.MemoryEngine;
import com.example.aidatingagentbackend.engine.RelationshipEngine;
import com.example.aidatingagentbackend.entity.Memory;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.entity.State;
import com.example.aidatingagentbackend.prompt.PromptBuilder;
import com.example.aidatingagentbackend.repository.MemoryRepository;
import com.example.aidatingagentbackend.repository.RelationshipRepository;
import com.example.aidatingagentbackend.repository.StateRepository;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final PromptBuilder promptBuilder;
    private final OpenAiService openAiService;
    private final EmotionEngine emotionEngine;
    private final RelationshipEngine relationshipEngine;
    private final MemoryEngine memoryEngine;
    private final StateRepository stateRepository;
    private final RelationshipRepository relationshipRepository;
    private final MemoryRepository memoryRepository;

    public ChatService(
            PromptBuilder promptBuilder,
            OpenAiService openAiService,
            EmotionEngine emotionEngine,
            RelationshipEngine relationshipEngine,
            MemoryEngine memoryEngine,
            StateRepository stateRepository,
            RelationshipRepository relationshipRepository,
            MemoryRepository memoryRepository
    ) {
        this.promptBuilder = promptBuilder;
        this.openAiService = openAiService;
        this.emotionEngine = emotionEngine;
        this.relationshipEngine = relationshipEngine;
        this.memoryEngine = memoryEngine;
        this.stateRepository = stateRepository;
        this.relationshipRepository = relationshipRepository;
        this.memoryRepository = memoryRepository;
    }

    public ChatResponse chat(ChatRequest request) {
        String prompt = promptBuilder.builder()
                .userMessage(request.getMessage())
                .build();

        String reply = openAiService.generate(prompt);
        updateContextAfterConversation(request.getMessage(), reply);
        return new ChatResponse(reply);
    }

    private void updateContextAfterConversation(String userMessage, String reply) {
        State state = stateRepository.findTopByOrderByIdDesc()
                .orElseGet(State::new);
        Relationship relationship = relationshipRepository.findTopByOrderByIdDesc()
                .orElseGet(Relationship::new);

        EmotionEngine.EmotionResult emotionResult = emotionEngine.analyze(state, userMessage);
        RelationshipEngine.RelationshipResult relationshipResult = relationshipEngine.analyze(relationship, userMessage);

        state.setEmotion(emotionResult.emotion());
        state.setEmotionIntensity(emotionResult.emotionIntensity());
        stateRepository.save(state);

        relationship.setTrust(relationshipResult.trust());
        relationship.setCloseness(relationshipResult.closeness());
        relationshipRepository.save(relationship);

        String conversation = buildConversation(userMessage, reply);
        MemoryEngine.MemoryDecision memoryDecision = memoryEngine.analyze(
                conversation,
                emotionResult.emotion(),
                emotionResult.emotionIntensity()
        );
        if (Boolean.TRUE.equals(memoryDecision.shouldCreate())) {
            Memory memory = new Memory();
            memory.setType(memoryDecision.memoryType());
            memory.setSummary(memoryDecision.episodeSummary());
            memory.setImportance(memoryDecision.importance());
            memoryRepository.save(memory);
        }
    }

    private String buildConversation(String userMessage, String reply) {
        return "User: " + userMessage + "\nAssistant: " + reply;
    }
}
