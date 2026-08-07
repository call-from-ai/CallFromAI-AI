package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.MemoryChannel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ChatRequest {

    private String requestId;
    private Long characterId;
    private MemoryChannel channel;
    private ProactiveContactReason contactReason;
    private ProactiveRelationshipState relationshipState;
    private RecentResponse recentResponse;
    private String userName;
    private String userTimeZone;
    private OffsetDateTime localDateTime;
    private CharacterSnapshot character;
    private RelationshipSnapshot relationship;
    private List<ChatHistoryItem> history = List.of();

    private String message;

    public Long resolveCharacterId() {
        Long snapshotCharacterId = character == null ? null : character.characterId();
        if (characterId != null && snapshotCharacterId != null && !characterId.equals(snapshotCharacterId)) {
            throw new IllegalArgumentException("characterId must match character.characterId");
        }
        return characterId == null ? snapshotCharacterId : characterId;
    }

    public void validateForChat() {
        validateSnapshots();
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
        validateChannel();
    }

    public void validateForChat(boolean hasImage) {
        validateSnapshots();
        if ((message == null || message.isBlank()) && !hasImage) {
            throw new IllegalArgumentException("message or image is required");
        }
        validateChannel();
    }

    public void validateForProactive() {
        validateSnapshots();
    }

    public ChatRequest withMessage(String resolvedMessage) {
        ChatRequest copy = new ChatRequest();
        copy.setRequestId(requestId);
        copy.setCharacterId(characterId);
        copy.setChannel(channel);
        copy.setContactReason(contactReason);
        copy.setRelationshipState(relationshipState);
        copy.setRecentResponse(recentResponse);
        copy.setUserName(userName);
        copy.setUserTimeZone(userTimeZone);
        copy.setLocalDateTime(localDateTime);
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
        validateTemporalContext();
    }

    private void validateTemporalContext() {
        boolean hasTimeZone = userTimeZone != null && !userTimeZone.isBlank();
        boolean hasLocalDateTime = localDateTime != null;
        if (hasTimeZone != hasLocalDateTime) {
            throw new IllegalArgumentException("userTimeZone and localDateTime must be provided together");
        }
        if (hasTimeZone) {
            try {
                ZoneId.of(userTimeZone.strip());
            } catch (DateTimeException exception) {
                throw new IllegalArgumentException("userTimeZone must be a valid IANA time zone", exception);
            }
        }
    }

    private void validateChannel() {
        if (channel == null) {
            throw new IllegalArgumentException("channel is required");
        }
    }
}
