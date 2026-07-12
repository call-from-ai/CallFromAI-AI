package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import com.example.aidatingagentbackend.entity.RelationshipTemperatureBand;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class CharacterExampleToneTagPolicy {

    public double score(
            String toneTag,
            CharacterTraitProfile traits,
            RelationshipTemperatureBand temperatureBand,
            boolean jealousyContext,
            Set<CharacterExampleRelevantTraitPolicy.RelevantTrait> relevantTraits
    ) {
        String tag = normalize(toneTag);
        double score = 0.0;
        score += traitScore(tag, traits, relevantTraits);
        score += temperatureScore(tag, temperatureBand, jealousyContext);
        return score;
    }

    public boolean isStrongPossessive(String toneTag, String assistantExample) {
        String text = normalize(toneTag) + " " + normalize(assistantExample);
        return text.contains("possessive")
                || text.contains("dominant")
                || text.contains("openly-jealous")
                || text.contains("openly_jealous")
                || text.contains("소유")
                || text.contains("내 거")
                || text.contains("내꺼")
                || text.contains("다른 사람 만나지");
    }

    public boolean isJealousTone(String toneTag) {
        String tag = normalize(toneTag);
        return tag.contains("jealous") || tag.contains("질투");
    }

    public String family(String toneTag) {
        String tag = normalize(toneTag);
        if (tag.contains("jealous")) {
            return "jealous";
        }
        if (tag.contains("repair") || tag.contains("hurt") || tag.contains("boundary")) {
            return "conflict";
        }
        if (tag.contains("friendly") || tag.contains("warm") || tag.contains("soft")) {
            return "warm";
        }
        if (tag.contains("spicy") || tag.contains("confident") || tag.contains("dominant")) {
            return "spicy";
        }
        if (tag.contains("playful") || tag.contains("teasing") || tag.contains("flirty")) {
            return "playful";
        }
        return tag.isBlank() ? "unknown" : tag.split("[_-]")[0];
    }

    private double traitScore(
            String tag,
            CharacterTraitProfile traits,
            Set<CharacterExampleRelevantTraitPolicy.RelevantTrait> relevantTraits
    ) {
        CharacterTraitProfile resolved = traits == null ? defaultTraits() : traits;
        double score = 0.0;
        if (tag.contains("playful") || tag.contains("teasing") || tag.contains("flirty") || tag.contains("pushpull")) {
            score += relevantBonus(relevantTraits, CharacterExampleRelevantTraitPolicy.RelevantTrait.PLAYFULNESS, resolved.getPlayfulness());
            score += relevantBonus(relevantTraits, CharacterExampleRelevantTraitPolicy.RelevantTrait.HUMOR, resolved.getHumor()) * 0.6;
            score += relevantBonus(relevantTraits, CharacterExampleRelevantTraitPolicy.RelevantTrait.CONFIDENCE, resolved.getConfidence()) * 0.6;
        }
        if (tag.contains("jealous")) {
            score += relevantBonus(relevantTraits, CharacterExampleRelevantTraitPolicy.RelevantTrait.JEALOUSY, resolved.getJealousy());
            score += relevantBonus(relevantTraits, CharacterExampleRelevantTraitPolicy.RelevantTrait.EXPRESSIVENESS, resolved.getExpressiveness()) * 0.6;
            score += relevantBonus(relevantTraits, CharacterExampleRelevantTraitPolicy.RelevantTrait.ATTACHMENT, resolved.getAttachment()) * 0.5;
        }
        if (tag.contains("warm") || tag.contains("friendly") || tag.contains("soft") || tag.contains("affection")) {
            score += relevantBonus(relevantTraits, CharacterExampleRelevantTraitPolicy.RelevantTrait.AFFECTION, resolved.getAffection());
            score += relevantBonus(relevantTraits, CharacterExampleRelevantTraitPolicy.RelevantTrait.EMPATHY, resolved.getEmpathy()) * 0.6;
        }
        if (tag.contains("confident") || tag.contains("spicy") || tag.contains("dominant")) {
            score += relevantBonus(relevantTraits, CharacterExampleRelevantTraitPolicy.RelevantTrait.CONFIDENCE, resolved.getConfidence());
            score += relevantBonus(relevantTraits, CharacterExampleRelevantTraitPolicy.RelevantTrait.DOMINANCE, resolved.getDominance()) * 0.7;
        }
        if (tag.contains("repair") || tag.contains("hurt") || tag.contains("boundary")) {
            score += relevantBonus(relevantTraits, CharacterExampleRelevantTraitPolicy.RelevantTrait.EMOTIONAL_STABILITY, resolved.getEmotionalStability()) * 0.8;
            score += relevantBonus(relevantTraits, CharacterExampleRelevantTraitPolicy.RelevantTrait.EMPATHY, resolved.getEmpathy()) * 0.6;
            score += relevantBonus(relevantTraits, CharacterExampleRelevantTraitPolicy.RelevantTrait.EXPRESSIVENESS, resolved.getExpressiveness()) * 0.4;
        }
        return score;
    }

    private double relevantBonus(
            Set<CharacterExampleRelevantTraitPolicy.RelevantTrait> relevantTraits,
            CharacterExampleRelevantTraitPolicy.RelevantTrait trait,
            Integer value
    ) {
        if (relevantTraits == null || !relevantTraits.contains(trait)) {
            return 0.0;
        }
        return highTraitBonus(value);
    }

    private double temperatureScore(
            String tag,
            RelationshipTemperatureBand band,
            boolean jealousyContext
    ) {
        RelationshipTemperatureBand resolved = band == null ? RelationshipTemperatureBand.PLAYFUL_FLIRTING : band;
        return switch (resolved) {
            case CALM -> matchesAny(tag, "calm", "stable", "considerate", "neutral", "soft") ? 8.0 : 0.0;
            case FRIENDLY_AFFECTION -> matchesAny(tag, "warm", "gentle", "friendly", "soft", "affection") ? 8.0 : 0.0;
            case PLAYFUL_FLIRTING -> matchesAny(tag, "playful", "flirty", "teasing", "pushpull", "spicy") ? 7.0 : 0.0;
            case ACTIVE_AFFECTION_JEALOUSY -> {
                double score = matchesAny(tag, "expressive", "teasing", "affection", "spicy", "flirty") ? 8.0 : 0.0;
                yield score + (jealousyContext && isJealousTone(tag) ? 6.0 : 0.0);
            }
            case SPICY_LEADING -> {
                double score = matchesAny(tag, "confident", "provocative", "dominant", "spicy", "pushpull") ? 10.0 : 0.0;
                yield score + (jealousyContext && isJealousTone(tag) ? 6.0 : 0.0);
            }
        };
    }

    private boolean matchesAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private double highTraitBonus(Integer trait) {
        return Math.max(0, value(trait) - 5) * 2.0;
    }

    private int value(Integer value) {
        return value == null ? 5 : Math.max(0, Math.min(10, value));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private CharacterTraitProfile defaultTraits() {
        CharacterTraitProfile profile = new CharacterTraitProfile();
        profile.setHumor(5);
        profile.setPlayfulness(5);
        profile.setAffection(5);
        profile.setEmpathy(5);
        profile.setAttachment(5);
        profile.setJealousy(5);
        profile.setDominance(5);
        profile.setConfidence(5);
        profile.setExpressiveness(5);
        profile.setEmotionalStability(5);
        return profile;
    }
}
