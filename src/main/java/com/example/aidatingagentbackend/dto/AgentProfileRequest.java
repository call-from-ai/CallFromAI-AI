package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.AgentLifeType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AgentProfileRequest {

    private Long userId;

    private AgentLifeType lifeType;
}
