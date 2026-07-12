package com.example.aidatingagentbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ChatRequest {

    private String requestId;
    private CharacterSnapshot character;
    private RelationshipSnapshot relationship;
    private List<ChatHistoryItem> history = List.of();

    private String message;

    public Long resolveCharacterId() {
        return character == null ? null : character.characterId();
    }
}
