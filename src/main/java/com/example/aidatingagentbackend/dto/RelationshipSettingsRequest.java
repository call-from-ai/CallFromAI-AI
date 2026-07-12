package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RelationshipSettingsRequest {

    private String relationshipStage;

    private Integer relationshipTemperatureScore;

    private RelationshipTemperature relationshipTemperature;
}
