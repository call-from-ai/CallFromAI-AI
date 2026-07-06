package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.AgentSelfStateResponse;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.repository.AgentSelfStateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AgentSelfStateService {

    private final AgentSelfStateRepository agentSelfStateRepository;
    private final EmotionUpdateService emotionUpdateService;

    public AgentSelfStateService(
            AgentSelfStateRepository agentSelfStateRepository,
            EmotionUpdateService emotionUpdateService
    ) {
        this.agentSelfStateRepository = agentSelfStateRepository;
        this.emotionUpdateService = emotionUpdateService;
    }

    @Transactional(readOnly = true)
    public List<AgentSelfStateResponse> findAll() {
        return agentSelfStateRepository.findAll()
                .stream()
                .map(AgentSelfStateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgentSelfStateResponse findByCharacterId(Long characterId) {
        AgentSelfState state = agentSelfStateRepository.findByCharacterId(characterId)
                .orElseGet(() -> emotionUpdateService.findOrCreatePreview(characterId));

        return AgentSelfStateResponse.from(state);
    }

    @Transactional
    public void delete(Long id) {
        AgentSelfState state = agentSelfStateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent self state not found. id=" + id));
        agentSelfStateRepository.delete(state);
    }
}
