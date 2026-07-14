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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CharacterDerivedDataServiceTests {

    @Mock MemoryRepository memoryRepository;
    @Mock AgentSelfStateRepository agentSelfStateRepository;
    @Mock AgentSelfStateLogRepository agentSelfStateLogRepository;
    @Mock AgentWorldStateRepository agentWorldStateRepository;
    @Mock AgentGoalRepository agentGoalRepository;
    @Mock AgentLifeEventRepository agentLifeEventRepository;
    @Mock ConversationEventRepository conversationEventRepository;
    @Mock CharacterPreferenceRepository characterPreferenceRepository;
    @Mock CharacterExampleRepository characterExampleRepository;
    @Mock ResponseQualityEvaluationRepository responseQualityEvaluationRepository;
    @Mock TurningPointRepository turningPointRepository;
    @Mock CharacterSnapshotRepository characterSnapshotRepository;

    @InjectMocks CharacterDerivedDataService service;

    @Test
    void deletesEveryDerivedDataSetByCharacterId() {
        service.deleteAllForCharacter(42L);

        verify(memoryRepository).deleteByCharacterId(42L);
        verify(agentSelfStateRepository).deleteByCharacterId(42L);
        verify(agentSelfStateLogRepository).deleteByCharacterId(42L);
        verify(agentWorldStateRepository).deleteByCharacterId(42L);
        verify(agentGoalRepository).deleteByCharacterId(42L);
        verify(agentLifeEventRepository).deleteByCharacterId(42L);
        verify(conversationEventRepository).deleteByCharacterId(42L);
        verify(characterPreferenceRepository).deleteByCharacterId(42L);
        verify(characterExampleRepository).deleteByCharacterId(42L);
        verify(responseQualityEvaluationRepository).deleteByCharacterId(42L);
        verify(turningPointRepository).deleteByCharacterId(42L);
        verify(characterSnapshotRepository).deleteByCharacterId(42L);
    }
}
