package com.example.aidatingagentbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RelationshipRequest {

    private Long characterId;

    private Integer trust;

    private Integer closeness;

    private Integer conflictLevel;

    private Integer repairProgress;

    private Integer breakupRisk;

    private String relationshipStage;

    private Integer relationshipTemperatureScore;

    private Integer daysTogether;
}
