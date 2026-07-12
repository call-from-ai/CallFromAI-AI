package com.example.aidatingagentbackend.domain;

public record CharacterTrait(
        int humor,
        int playfulness,
        int affection,
        int empathy,
        int attachment,
        int jealousy,
        int dominance,
        int confidence,
        int expressiveness,
        int emotionalStability
) {

    public static final int MIN_VALUE = 0;
    public static final int MAX_VALUE = 10;
    public static final int DEFAULT_VALUE = 3;

    public static CharacterTrait defaults() {
        return new CharacterTrait(
                DEFAULT_VALUE,
                DEFAULT_VALUE,
                DEFAULT_VALUE,
                DEFAULT_VALUE,
                DEFAULT_VALUE,
                DEFAULT_VALUE,
                DEFAULT_VALUE,
                DEFAULT_VALUE,
                DEFAULT_VALUE,
                DEFAULT_VALUE
        );
    }
}
