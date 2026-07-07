package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.AgentLifeType;
import com.example.aidatingagentbackend.entity.AgentProfile;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AgentProfileResponse {

    private Long id;

    private Long userId;

    private AgentLifeType lifeType;

    public static AgentProfileResponse from(AgentProfile profile) {
        AgentProfileResponse response = new AgentProfileResponse();
        response.setId(profile.getId());
        response.setUserId(profile.getUserId());
        response.setLifeType(profile.getLifeType());
        return response;
    }
}
