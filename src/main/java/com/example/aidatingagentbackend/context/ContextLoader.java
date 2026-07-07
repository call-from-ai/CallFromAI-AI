package com.example.aidatingagentbackend.context;

import com.example.aidatingagentbackend.dto.Context;
import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.entity.Character;
import com.example.aidatingagentbackend.entity.AgentGoal;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.AgentWorldState;
import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.entity.State;
import com.example.aidatingagentbackend.repository.AgentSelfStateRepository;
import com.example.aidatingagentbackend.repository.CharacterRepository;
import com.example.aidatingagentbackend.repository.ChatMessageRepository;
import com.example.aidatingagentbackend.repository.RelationshipRepository;
import com.example.aidatingagentbackend.repository.StateRepository;
import com.example.aidatingagentbackend.repository.TurningPointRepository;
import com.example.aidatingagentbackend.service.ReflectionService;
import com.example.aidatingagentbackend.service.AgentGoalService;
import com.example.aidatingagentbackend.service.AgentInitiativeService;
import com.example.aidatingagentbackend.service.AgentProfileService;
import com.example.aidatingagentbackend.service.AgentWorldStateService;
import com.example.aidatingagentbackend.service.CharacterExampleService;
import org.springframework.stereotype.Service;

@Service
public class ContextLoader {

    private final CharacterRepository characterRepository;
    private final StateRepository stateRepository;
    private final RelationshipRepository relationshipRepository;
    private final AgentSelfStateRepository agentSelfStateRepository;
    private final MemoryRetrievalService memoryRetrievalService;
    private final ReflectionService reflectionService;
    private final TurningPointRepository turningPointRepository;
    private final ChatMessageRepository chatRepository;
    private final CharacterExampleService characterExampleService;
    private final AgentProfileService agentProfileService;
    private final AgentWorldStateService agentWorldStateService;
    private final AgentGoalService agentGoalService;
    private final AgentInitiativeService agentInitiativeService;

    public ContextLoader(
            CharacterRepository characterRepository,
            StateRepository stateRepository,
            RelationshipRepository relationshipRepository,
            AgentSelfStateRepository agentSelfStateRepository,
            MemoryRetrievalService memoryRetrievalService,
            ReflectionService reflectionService,
            TurningPointRepository turningPointRepository,
            ChatMessageRepository chatRepository,
            CharacterExampleService characterExampleService,
            AgentProfileService agentProfileService,
            AgentWorldStateService agentWorldStateService,
            AgentGoalService agentGoalService,
            AgentInitiativeService agentInitiativeService
    ) {
        this.characterRepository = characterRepository;
        this.stateRepository = stateRepository;
        this.relationshipRepository = relationshipRepository;
        this.agentSelfStateRepository = agentSelfStateRepository;
        this.memoryRetrievalService = memoryRetrievalService;
        this.reflectionService = reflectionService;
        this.turningPointRepository = turningPointRepository;
        this.chatRepository = chatRepository;
        this.characterExampleService = characterExampleService;
        this.agentProfileService = agentProfileService;
        this.agentWorldStateService = agentWorldStateService;
        this.agentGoalService = agentGoalService;
        this.agentInitiativeService = agentInitiativeService;
    }

    public Context load(Long characterId, String userMessage) {
        return load(characterId, userMessage, AgentEventType.NORMAL, RelationshipTemperature.NEUTRAL);
    }

    public Context load(
            Long characterId,
            String userMessage,
            AgentEventType eventType,
            RelationshipTemperature relationshipTemperature
    ) {
        AgentEventType resolvedEventType = eventType == null ? AgentEventType.NORMAL : eventType;
        RelationshipTemperature resolvedTemperature = relationshipTemperature == null
                ? RelationshipTemperature.NEUTRAL
                : relationshipTemperature;

        Character character =
                characterRepository.findById(characterId)
                        .orElse(null);

        State state =
                stateRepository.findTopByOrderByIdDesc()
                        .orElse(new State());

        Relationship relationship =
                relationshipRepository.findTopByOrderByIdDesc()
                        .orElse(new Relationship());

        AgentSelfState agentSelfState = agentSelfStateRepository.findByCharacterId(characterId)
                .orElse(null);
        AgentWorldState agentWorldState = agentWorldStateService.findByUserId(characterId);
        AgentGoal agentGoal = agentGoalService.findCurrentGoal(characterId);

        return new Context(

                character,

                state,

                relationship,

                agentSelfState,

                agentProfileService.findOrDefault(characterId),

                agentWorldState,

                agentGoal,

                agentInitiativeService.plan(userMessage, resolvedTemperature, agentSelfState, agentWorldState, agentGoal, relationship),

                resolvedTemperature,

                characterExampleService.findRelevantEntities(characterId, resolvedEventType, resolvedTemperature),

                memoryRetrievalService.retrieve(userMessage, state),

                reflectionService.findRelevantForPrompt(characterId),

                turningPointRepository.findTop10ByOrderByCreatedAtDesc(),

                chatRepository
                        .findTop20ByCharacterIdOrderByCreatedAtDesc(characterId)

        );

    }

}
