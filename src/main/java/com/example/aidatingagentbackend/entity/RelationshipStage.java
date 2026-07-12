package com.example.aidatingagentbackend.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RelationshipStage {
    CRUSH,
    DATING,
    DEEP_LOVE,
    @Deprecated
    EARLY_DATING,
    @Deprecated
    LONG_TERM

    ;

    @JsonCreator
    public static RelationshipStage from(String value) {
        if (value == null) return null;
        return switch (value.trim()) {
            case "썸", "CRUSH" -> CRUSH;
            case "연애", "DATING", "EARLY_DATING" -> DATING;
            case "깊은 사랑", "DEEP_LOVE", "LONG_TERM" -> DEEP_LOVE;
            default -> throw new IllegalArgumentException("Unsupported relationshipStage: " + value);
        };
    }
}
