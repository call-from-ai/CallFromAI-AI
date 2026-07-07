package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.AgentProfileRequest;
import com.example.aidatingagentbackend.dto.AgentProfileResponse;
import com.example.aidatingagentbackend.entity.AgentLifeType;
import com.example.aidatingagentbackend.entity.AgentProfile;
import com.example.aidatingagentbackend.repository.AgentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentProfileService {

    private static final AgentLifeType DEFAULT_LIFE_TYPE = AgentLifeType.WORKER;

    private final AgentProfileRepository agentProfileRepository;

    public AgentProfileService(AgentProfileRepository agentProfileRepository) {
        this.agentProfileRepository = agentProfileRepository;
    }

    @Transactional
    public AgentProfileResponse save(AgentProfileRequest request) {
        AgentProfile profile = agentProfileRepository.findByUserId(request.getUserId())
                .orElseGet(() -> createDefaultProfile(request.getUserId()));
        profile.setLifeType(request.getLifeType() == null ? DEFAULT_LIFE_TYPE : request.getLifeType());
        return AgentProfileResponse.from(agentProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public AgentProfile findOrDefault(Long userId) {
        return agentProfileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));
    }

    @Transactional(readOnly = true)
    public AgentProfileResponse findByUserId(Long userId) {
        return AgentProfileResponse.from(findOrDefault(userId));
    }

    private AgentProfile createDefaultProfile(Long userId) {
        AgentProfile profile = new AgentProfile();
        profile.setUserId(userId);
        profile.setLifeType(DEFAULT_LIFE_TYPE);
        return profile;
    }
}
