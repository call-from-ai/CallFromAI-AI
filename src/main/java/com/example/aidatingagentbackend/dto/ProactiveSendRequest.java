package com.example.aidatingagentbackend.dto;
import java.util.List;
public record ProactiveSendRequest(String requestId, CharacterSnapshot character,
        RelationshipSnapshot relationship, List<ChatHistoryItem> history) {
    public ProactiveSendRequest {
        if (character == null) throw new IllegalArgumentException("character snapshot is required");
        if (relationship == null) throw new IllegalArgumentException("relationship snapshot is required");
        history = history == null ? List.of() : List.copyOf(history);
    }
}
