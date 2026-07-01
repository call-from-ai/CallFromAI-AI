package com.example.aidatingagentbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RelationshipRequest {

    private Integer trust;

    private Integer closeness;

    private String relationshipStage;

    private Integer daysTogether;
}
