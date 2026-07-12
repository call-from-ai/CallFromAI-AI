package com.example.aidatingagentbackend.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SpeechStyle {
    CASUAL, SEMI_FORMAL, FORMAL;

    @JsonCreator
    public static SpeechStyle from(String value) {
        if (value == null) return null;
        return switch (value.trim()) {
            case "반말", "CASUAL" -> CASUAL;
            case "반존대", "SEMI_FORMAL" -> SEMI_FORMAL;
            case "존댓말", "FORMAL" -> FORMAL;
            default -> throw new IllegalArgumentException("Unsupported speechStyle: " + value);
        };
    }
}
