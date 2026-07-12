package com.example.aidatingagentbackend.context;

import com.example.aidatingagentbackend.dto.*;
import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.entity.*;
import com.example.aidatingagentbackend.repository.AgentSelfStateRepository;
import com.example.aidatingagentbackend.repository.StateRepository;
import com.example.aidatingagentbackend.service.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContextLoader {
    private final StateRepository stateRepository;
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

    public ContextLoader(StateRepository stateRepository, AgentSelfStateRepository agentSelfStateRepository,
            MemoryRetrievalService memoryRetrievalService, CharacterExampleService characterExampleService,
            AgentWorldStateService agentWorldStateService, AgentGoalService agentGoalService,
            AgentInitiativeService agentInitiativeService, AgentLifeEventService agentLifeEventService,
            ConversationEventService conversationEventService, CharacterPreferenceService characterPreferenceService,
            ConversationTopicService conversationTopicService) {
        this.stateRepository = stateRepository;
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
    }

    public Context load(ChatRequest request, EventAnalysis analysis, ContextUpdater.RelationshipUpdate relationshipUpdate) {
        CharacterSnapshot character = requireCharacter(request);
        RelationshipSnapshot relationship = relationshipUpdate.nextRelationship();
        Long characterId = character.characterId();
        State state = stateRepository.findByCharacterId(characterId).orElseGet(() -> defaultState(characterId));
        AgentSelfState selfState = agentSelfStateRepository.findByCharacterId(characterId).orElse(null);
        AgentWorldState worldState = agentWorldStateService.findByUserId(characterId);
        AgentGoal goal = agentGoalService.findCurrentGoal(characterId);
        List<AgentLifeEvent> lifeEvents = agentLifeEventService.ensureAndFindForPrompt(characterId, character);
        PreferenceQuestionPlan preferencePlan = characterPreferenceService.plan(characterId, request.getMessage());
        EventAnalysis resolved = analysis == null ? EventAnalysis.fallback(AgentEventType.NORMAL) : analysis;
        CharacterTraitSnapshot traits = character.traits();

        return new Context(character, state, relationship, relationshipUpdate.delta(), traits,
                relationship.relationshipStage(), relationship.relationshipTemperatureScore(), character.romanceStyleScore(),
                selfState, worldState, goal,
                agentInitiativeService.plan(request.getMessage(), relationship.strategy(), selfState, worldState, goal, lifeEvents),
                relationship.strategy(), lifeEvents, conversationEventService.findRecentForPrompt(characterId),
                preferencePlan, conversationTopicService.plan(request.getMessage(), preferencePlan),
                characterPreferenceService.findForPrompt(characterId, preferencePlan),
                characterExampleService.findRelevantEntities(characterId, resolved, relationship.strategy(),
                        relationship.relationshipStage(), relationship.relationshipTemperatureScore(), character.romanceStyleScore(), traits),
                preferencePlan.active() ? List.of() : memoryRetrievalService.retrieve(characterId, request.getMessage(), state,
                        traits, relationship.relationshipStage(), relationship.relationshipTemperatureScore(), resolved),
                request.getHistory() == null ? List.of() : request.getHistory());
    }

    private CharacterSnapshot requireCharacter(ChatRequest request) {
        if (request == null || request.getCharacter() == null) throw new IllegalArgumentException("character snapshot is required");
        if (request.getRelationship() == null) throw new IllegalArgumentException("relationship snapshot is required");
        return request.getCharacter();
    }

    private State defaultState(Long characterId) {
        State state = new State(); state.setCharacterId(characterId); state.setEmotion("neutral");
        state.setEmotionIntensity(0); state.setEnergy(50); state.setStress(20); return state;
    }
}
