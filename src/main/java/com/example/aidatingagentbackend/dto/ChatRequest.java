package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.MemoryChannel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;
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
    private UserSnapshot user;
    // Legacy top-level fields remain supported during contract migration.
    private Integer userAge;
    private String userGender;
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
        copy.setUser(user);
        copy.setUserAge(userAge);
        copy.setUserGender(userGender);
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
        Integer resolvedUserAge = getUserAge();
        if (resolvedUserAge != null && (resolvedUserAge < 1 || resolvedUserAge > 120)) {
            throw new IllegalArgumentException("userAge must be between 1 and 120");
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

    public Integer getUserAge() {
        // The legacy backend uses 0 to represent an unknown age.
        // Prefer a birth-derived age when available; otherwise expose it as missing.
        if (userAge != null && userAge != 0) return userAge;
        if (user == null || user.birth() == null) return null;

        LocalDate referenceDate = localDateTime == null
                ? LocalDate.now()
                : localDateTime.toLocalDate();
        return Period.between(user.birth(), referenceDate).getYears();
    }

    public String getUserGender() {
        if (userGender != null && !userGender.isBlank()) return userGender;
        return user == null ? null : user.gender();
    }
}
