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

    private Integer trust;

    private Integer closeness;

    private String relationshipStage;

    private Integer daysTogether;

    public static RelationshipResponse from(Relationship relationship) {
        RelationshipResponse response = new RelationshipResponse();
        response.setId(relationship.getId());
        response.setTrust(relationship.getTrust());
        response.setCloseness(relationship.getCloseness());
        response.setRelationshipStage(relationship.getRelationshipStage());
        response.setDaysTogether(relationship.getDaysTogether());
        return response;
    }
}
