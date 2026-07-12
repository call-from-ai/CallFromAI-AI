package com.example.aidatingagentbackend.domain;

public record EmotionDelta(
        double affection,
        double trust,
        double hurt,
        double anger,
        double insecurity,
        double disappointment,
        double emotionalDistance
) {

    public static EmotionDelta none() {
        return new EmotionDelta(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    public EmotionDelta multiply(
            double affectionModifier,
            double hurtModifier,
            double angerModifier,
            double insecurityModifier,
            double disappointmentModifier,
            double emotionalDistanceModifier
    ) {
        return new EmotionDelta(
                affection * affectionModifier,
                trust,
                hurt * hurtModifier,
                anger * angerModifier,
                insecurity * insecurityModifier,
                disappointment * disappointmentModifier,
                emotionalDistance * emotionalDistanceModifier
        );
    }

    public EmotionDelta plus(EmotionDelta other) {
        return new EmotionDelta(
                affection + other.affection,
                trust + other.trust,
                hurt + other.hurt,
                anger + other.anger,
                insecurity + other.insecurity,
                disappointment + other.disappointment,
                emotionalDistance + other.emotionalDistance
        );
    }
}
