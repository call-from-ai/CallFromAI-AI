package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import com.example.aidatingagentbackend.entity.RelationshipTemperatureBand;
import org.springframework.stereotype.Service;

@Service
public class RelationshipTemperatureScoreResolver {

    public static final int MIN_SCORE = 0;
    public static final int MAX_SCORE = 100;

    private final SettingsDefaultPolicy settingsDefaultPolicy;

    public RelationshipTemperatureScoreResolver(SettingsDefaultPolicy settingsDefaultPolicy) {
        this.settingsDefaultPolicy = settingsDefaultPolicy;
    }

    public Integer validate(Integer score) {
        if (score == null) {
            return null;
        }
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new IllegalArgumentException("relationshipTemperatureScore must be between 0 and 100.");
        }
        return score;
    }

    public RelationshipTemperatureBand resolveBand(Integer score) {
        int resolvedScore = score == null ? settingsDefaultPolicy.defaultRelationshipTemperatureScore() : validate(score);
        if (resolvedScore <= 20) {
            return RelationshipTemperatureBand.CALM;
        }
        if (resolvedScore <= 40) {
            return RelationshipTemperatureBand.FRIENDLY_AFFECTION;
        }
        if (resolvedScore <= 60) {
            return RelationshipTemperatureBand.PLAYFUL_FLIRTING;
        }
        if (resolvedScore <= 80) {
            return RelationshipTemperatureBand.ACTIVE_AFFECTION_JEALOUSY;
        }
        return RelationshipTemperatureBand.SPICY_LEADING;
    }

    public Integer defaultScoreForLegacy(RelationshipTemperature temperature) {
        if (temperature == null) {
            return settingsDefaultPolicy.defaultRelationshipTemperatureScore();
        }
        return switch (temperature) {
            case FRIENDLY -> 35;
            case NEUTRAL -> 50;
            case SPICY -> 85;
            case CONFLICT_REPAIR -> 50;
        };
    }

    public Integer resolveScore(Integer score, RelationshipTemperature legacyTemperature) {
        if (score != null) {
            return validate(score);
        }
        if (legacyTemperature != null) {
            return defaultScoreForLegacy(legacyTemperature);
        }
        return settingsDefaultPolicy.defaultRelationshipTemperatureScore();
    }
}
