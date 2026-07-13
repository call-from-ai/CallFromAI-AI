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

    public void validateForChat() {
        validateSnapshots();
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
    }

    public void validateForProactive() {
        validateSnapshots();
    }

    public ChatRequest withMessage(String resolvedMessage) {
        ChatRequest copy = new ChatRequest();
        copy.setRequestId(requestId);
        copy.setCharacter(character);
        copy.setRelationship(relationship);
        copy.setHistory(history == null ? List.of() : List.copyOf(history));
        copy.setMessage(resolvedMessage);
        return copy;
    }

    private void validateSnapshots() {
        if (character == null) {
            throw new IllegalArgumentException("character snapshot is required");
        }
        if (relationship == null) {
            throw new IllegalArgumentException("relationship snapshot is required");
        }
    }
}
