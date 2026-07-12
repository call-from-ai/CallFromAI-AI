package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.Relationship;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RelationshipSettingsResponse {

    private Long relationshipId;

    private Long characterId;

    private String relationshipStage;

    private Integer relationshipTemperatureScore;

    public static RelationshipSettingsResponse from(
            Relationship relationship,
            String relationshipStage,
            Integer relationshipTemperatureScore
    ) {
        RelationshipSettingsResponse response = new RelationshipSettingsResponse();
        response.setRelationshipId(relationship.getId());
        response.setCharacterId(relationship.getCharacterId());
        response.setRelationshipStage(relationshipStage);
        response.setRelationshipTemperatureScore(relationshipTemperatureScore);
        return response;
    }
}
