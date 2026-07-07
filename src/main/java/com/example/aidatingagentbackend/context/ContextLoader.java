package com.example.aidatingagentbackend.context;

import com.example.aidatingagentbackend.dto.Context;
import com.example.aidatingagentbackend.entity.Character;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.entity.State;
import com.example.aidatingagentbackend.repository.AgentSelfStateRepository;
import com.example.aidatingagentbackend.repository.CharacterRepository;
import com.example.aidatingagentbackend.repository.CharacterExampleRepository;
import com.example.aidatingagentbackend.repository.ChatMessageRepository;
import com.example.aidatingagentbackend.repository.RelationshipRepository;
import com.example.aidatingagentbackend.repository.StateRepository;
import com.example.aidatingagentbackend.repository.TurningPointRepository;
import com.example.aidatingagentbackend.service.ReflectionService;
import com.example.aidatingagentbackend.service.AgentGoalService;
import com.example.aidatingagentbackend.service.AgentProfileService;
import com.example.aidatingagentbackend.service.AgentWorldStateService;
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
    private final CharacterExampleRepository characterExampleRepository;
    private final AgentProfileService agentProfileService;
    private final AgentWorldStateService agentWorldStateService;
    private final AgentGoalService agentGoalService;

    public ContextLoader(
            CharacterRepository characterRepository,
            StateRepository stateRepository,
            RelationshipRepository relationshipRepository,
            AgentSelfStateRepository agentSelfStateRepository,
            MemoryRetrievalService memoryRetrievalService,
            ReflectionService reflectionService,
            TurningPointRepository turningPointRepository,
            ChatMessageRepository chatRepository,
            CharacterExampleRepository characterExampleRepository,
            AgentProfileService agentProfileService,
            AgentWorldStateService agentWorldStateService,
            AgentGoalService agentGoalService
    ) {
        this.characterRepository = characterRepository;
        this.stateRepository = stateRepository;
        this.relationshipRepository = relationshipRepository;
        this.agentSelfStateRepository = agentSelfStateRepository;
        this.memoryRetrievalService = memoryRetrievalService;
        this.reflectionService = reflectionService;
        this.turningPointRepository = turningPointRepository;
        this.chatRepository = chatRepository;
        this.characterExampleRepository = characterExampleRepository;
        this.agentProfileService = agentProfileService;
        this.agentWorldStateService = agentWorldStateService;
        this.agentGoalService = agentGoalService;
    }

    public Context load(Long characterId, String userMessage) {

        Character character =
                characterRepository.findById(characterId)
                        .orElse(null);

        State state =
                stateRepository.findTopByOrderByIdDesc()
                        .orElse(new State());

        Relationship relationship =
                relationshipRepository.findTopByOrderByIdDesc()
                        .orElse(new Relationship());

        return new Context(

                character,

                state,

                relationship,

                agentSelfStateRepository.findByCharacterId(characterId)
                        .orElse(null),

                agentProfileService.findOrDefault(characterId),

                agentWorldStateService.findByUserId(characterId),

                agentGoalService.findCurrentGoal(characterId),

                characterExampleRepository.findTop5ByCharacterIdOrderByPriorityDescIdAsc(characterId),

                memoryRetrievalService.retrieve(userMessage, state),

                reflectionService.findRelevantForPrompt(characterId),

                turningPointRepository.findTop10ByOrderByCreatedAtDesc(),

                chatRepository
                        .findTop20ByCharacterIdOrderByCreatedAtDesc(characterId)

        );

    }

}
