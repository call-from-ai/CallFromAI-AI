package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CharacterExampleRequest {

    private Long characterId;

    private AgentEventType eventType;

    private RelationshipTemperature relationshipTemperature;

    private String relationshipStage;

    private Integer minTemperatureScore;

    private Integer maxTemperatureScore;

    private String userExample;

    private String assistantExample;

    private String toneTag;

    private Integer priority;

    private Boolean active;
}
