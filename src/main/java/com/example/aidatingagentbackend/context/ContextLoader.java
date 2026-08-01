package com.example.aidatingagentbackend.context;

import com.example.aidatingagentbackend.dto.*;
import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.entity.*;
import com.example.aidatingagentbackend.repository.AgentSelfStateRepository;
import com.example.aidatingagentbackend.service.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContextLoader {
    private final AgentSelfStateRepository agentSelfStateRepository;
    private final MemoryRetrievalService memoryRetrievalService;
    private final CharacterExampleService characterExampleService;
    private final AgentWorldStateService agentWorldStateService;
    private final AgentGoalService agentGoalService;
    private final AgentInitiativeService agentInitiativeService;
    private final AgentLifeEventService agentLifeEventService;
    private final ConversationEventService conversationEventService;
    private final CharacterPreferenceService characterPreferenceService;
    private final ConversationTopicService conversationTopicService;
    private final ConversationMemoryService conversationMemoryService;

    public ContextLoader(AgentSelfStateRepository agentSelfStateRepository,
            MemoryRetrievalService memoryRetrievalService, CharacterExampleService characterExampleService,
            AgentWorldStateService agentWorldStateService, AgentGoalService agentGoalService,
            AgentInitiativeService agentInitiativeService, AgentLifeEventService agentLifeEventService,
            ConversationEventService conversationEventService, CharacterPreferenceService characterPreferenceService,
            ConversationTopicService conversationTopicService,
            ConversationMemoryService conversationMemoryService) {
        this.agentSelfStateRepository = agentSelfStateRepository;
        this.memoryRetrievalService = memoryRetrievalService;
        this.characterExampleService = characterExampleService;
        this.agentWorldStateService = agentWorldStateService;
        this.agentGoalService = agentGoalService;
        this.agentInitiativeService = agentInitiativeService;
        this.agentLifeEventService = agentLifeEventService;
        this.conversationEventService = conversationEventService;
        this.characterPreferenceService = characterPreferenceService;
        this.conversationTopicService = conversationTopicService;
        this.conversationMemoryService = conversationMemoryService;
    }

    public Context load(ChatRequest request, EventAnalysis analysis, ContextUpdater.RelationshipUpdate relationshipUpdate) {
        CharacterSnapshot character = requireCharacter(request);
        RelationshipSnapshot relationship = relationshipUpdate.nextRelationship();
        Long characterId = character.characterId();
        AgentSelfState selfState = agentSelfStateRepository.findByCharacterId(characterId).orElse(null);
        AgentWorldState worldState = agentWorldStateService.findByCharacterId(characterId);
        AgentGoal goal = agentGoalService.findCurrentGoal(characterId);
        List<AgentLifeEvent> lifeEvents = agentLifeEventService.ensureAndFindForPrompt(characterId, character);
        PreferenceQuestionPlan preferencePlan = characterPreferenceService.plan(characterId, request.getMessage());
        EventAnalysis resolved = analysis == null ? EventAnalysis.fallback(AgentEventType.NORMAL) : analysis;
        CharacterTraitSnapshot traits = character.traits();
        List<ChatHistoryItem> unifiedHistory = conversationMemoryService.findRecentHistory(characterId);
        List<ChatHistoryItem> promptHistory = unifiedHistory.isEmpty()
                ? (request.getHistory() == null ? List.of() : request.getHistory())
                : unifiedHistory;

        return new Context(character, relationship, traits,
                relationship.relationshipStage(), relationship.relationshipTemperatureScore(), character.romanceStyleScore(),
                selfState, worldState, goal,
                agentInitiativeService.plan(request.getMessage(), relationship.strategy(), selfState, worldState, goal, lifeEvents),
                relationship.strategy(), lifeEvents, conversationEventService.findRecentForPrompt(characterId),
                preferencePlan, conversationTopicService.plan(request.getMessage(), preferencePlan),
                characterPreferenceService.findForPrompt(characterId, preferencePlan),
                characterExampleService.findRelevantEntities(characterId, resolved, relationship.strategy(),
                        relationship.relationshipStage(), relationship.relationshipTemperatureScore(), character.romanceStyleScore(), traits),
                preferencePlan.active() ? List.of() : memoryRetrievalService.retrieve(characterId, request.getMessage(), selfState,
                        traits, relationship.relationshipStage(), relationship.relationshipTemperatureScore(), resolved),
                promptHistory);
    }

    private CharacterSnapshot requireCharacter(ChatRequest request) {
        if (request == null || request.getCharacter() == null) throw new IllegalArgumentException("character snapshot is required");
        if (request.getRelationship() == null) throw new IllegalArgumentException("relationship snapshot is required");
        return request.getCharacter();
    }

}
