package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.AgentSelfStateLogResponse;
import com.example.aidatingagentbackend.repository.AgentSelfStateLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AgentSelfStateLogService {

    private final AgentSelfStateLogRepository agentSelfStateLogRepository;

    public AgentSelfStateLogService(AgentSelfStateLogRepository agentSelfStateLogRepository) {
        this.agentSelfStateLogRepository = agentSelfStateLogRepository;
    }

    @Transactional(readOnly = true)
    public List<AgentSelfStateLogResponse> findRecentByUserId(Long userId) {
        return agentSelfStateLogRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(AgentSelfStateLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AgentSelfStateLogResponse> findAll() {
        return agentSelfStateLogRepository.findAll()
                .stream()
                .map(AgentSelfStateLogResponse::from)
                .toList();
    }
}
