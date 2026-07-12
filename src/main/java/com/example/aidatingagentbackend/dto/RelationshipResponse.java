package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.Relationship;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RelationshipResponse {

    private Long id;

    private Long characterId;

    private Integer trust;

    private Integer closeness;

    private Integer conflictLevel;

    private Integer repairProgress;

    private Integer breakupRisk;

    private String relationshipStage;

    private Integer relationshipTemperatureScore;

    private Integer daysTogether;

    public static RelationshipResponse from(Relationship relationship) {
        RelationshipResponse response = new RelationshipResponse();
        response.setId(relationship.getId());
        response.setCharacterId(relationship.getCharacterId());
        response.setTrust(relationship.getTrust());
        response.setCloseness(relationship.getCloseness());
        response.setConflictLevel(relationship.getConflictLevel());
        response.setRepairProgress(relationship.getRepairProgress());
        response.setBreakupRisk(relationship.getBreakupRisk());
        response.setRelationshipStage(relationship.getRelationshipStage());
        response.setRelationshipTemperatureScore(relationship.getRelationshipTemperatureScore());
        response.setDaysTogether(relationship.getDaysTogether());
        return response;
    }
}
