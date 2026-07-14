package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.repository.AgentGoalRepository;
import com.example.aidatingagentbackend.repository.AgentLifeEventRepository;
import com.example.aidatingagentbackend.repository.AgentSelfStateLogRepository;
import com.example.aidatingagentbackend.repository.AgentSelfStateRepository;
import com.example.aidatingagentbackend.repository.AgentWorldStateRepository;
import com.example.aidatingagentbackend.repository.CharacterExampleRepository;
import com.example.aidatingagentbackend.repository.CharacterPreferenceRepository;
import com.example.aidatingagentbackend.repository.CharacterSnapshotRepository;
import com.example.aidatingagentbackend.repository.ConversationEventRepository;
import com.example.aidatingagentbackend.repository.MemoryRepository;
import com.example.aidatingagentbackend.repository.ResponseQualityEvaluationRepository;
import com.example.aidatingagentbackend.repository.TurningPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CharacterDerivedDataService {

    private final MemoryRepository memoryRepository;
    private final AgentSelfStateRepository agentSelfStateRepository;
    private final AgentSelfStateLogRepository agentSelfStateLogRepository;
    private final AgentWorldStateRepository agentWorldStateRepository;
    private final AgentGoalRepository agentGoalRepository;
    private final AgentLifeEventRepository agentLifeEventRepository;
    private final ConversationEventRepository conversationEventRepository;
    private final CharacterPreferenceRepository characterPreferenceRepository;
    private final CharacterExampleRepository characterExampleRepository;
    private final ResponseQualityEvaluationRepository responseQualityEvaluationRepository;
    private final TurningPointRepository turningPointRepository;
    private final CharacterSnapshotRepository characterSnapshotRepository;

    public CharacterDerivedDataService(
            MemoryRepository memoryRepository,
            AgentSelfStateRepository agentSelfStateRepository,
            AgentSelfStateLogRepository agentSelfStateLogRepository,
            AgentWorldStateRepository agentWorldStateRepository,
            AgentGoalRepository agentGoalRepository,
            AgentLifeEventRepository agentLifeEventRepository,
            ConversationEventRepository conversationEventRepository,
            CharacterPreferenceRepository characterPreferenceRepository,
            CharacterExampleRepository characterExampleRepository,
            ResponseQualityEvaluationRepository responseQualityEvaluationRepository,
            TurningPointRepository turningPointRepository,
            CharacterSnapshotRepository characterSnapshotRepository
    ) {
        this.memoryRepository = memoryRepository;
        this.agentSelfStateRepository = agentSelfStateRepository;
        this.agentSelfStateLogRepository = agentSelfStateLogRepository;
        this.agentWorldStateRepository = agentWorldStateRepository;
        this.agentGoalRepository = agentGoalRepository;
        this.agentLifeEventRepository = agentLifeEventRepository;
        this.conversationEventRepository = conversationEventRepository;
        this.characterPreferenceRepository = characterPreferenceRepository;
        this.characterExampleRepository = characterExampleRepository;
        this.responseQualityEvaluationRepository = responseQualityEvaluationRepository;
        this.turningPointRepository = turningPointRepository;
        this.characterSnapshotRepository = characterSnapshotRepository;
    }

    @Transactional
    public void deleteAllForCharacter(Long characterId) {
        if (characterId == null || characterId <= 0) {
            throw new IllegalArgumentException("characterId must be positive");
        }

        memoryRepository.deleteByCharacterId(characterId);
        agentSelfStateLogRepository.deleteByCharacterId(characterId);
        agentSelfStateRepository.deleteByCharacterId(characterId);
        agentWorldStateRepository.deleteByCharacterId(characterId);
        agentGoalRepository.deleteByCharacterId(characterId);
        agentLifeEventRepository.deleteByCharacterId(characterId);
        conversationEventRepository.deleteByCharacterId(characterId);
        characterPreferenceRepository.deleteByCharacterId(characterId);
        characterExampleRepository.deleteByCharacterId(characterId);
        responseQualityEvaluationRepository.deleteByCharacterId(characterId);
        turningPointRepository.deleteByCharacterId(characterId);
        characterSnapshotRepository.deleteByCharacterId(characterId);
    }
}
