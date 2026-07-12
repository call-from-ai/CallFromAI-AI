package com.example.aidatingagentbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatResponse {

    private String requestId;
    private String reply;
    private RelationshipDelta relationshipDelta;
    private RelationshipSnapshot nextRelationship;
    private AgentSelfStateSnapshot previousAgentSelfState;
    private AgentSelfStateSnapshot nextAgentSelfState;
    private com.example.aidatingagentbackend.engine.EventAnalysis eventAnalysis;

    public ChatResponse(String reply) {
        this.reply = reply;
    }
}
