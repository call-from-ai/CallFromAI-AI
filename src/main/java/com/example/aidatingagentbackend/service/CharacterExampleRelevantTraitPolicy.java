package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

@Service
public class CharacterExampleRelevantTraitPolicy {

    public Set<RelevantTrait> select(EventAnalysis eventAnalysis) {
        AgentEventType eventType = eventAnalysis == null || eventAnalysis.eventType() == null
                ? AgentEventType.NORMAL
                : eventAnalysis.eventType();
        String context = normalize(eventAnalysis == null ? null : eventAnalysis.primaryEmotion())
                + " "
                + normalize(eventAnalysis == null ? null : eventAnalysis.summary());

        if (isJealousyContext(context)) {
            return EnumSet.of(RelevantTrait.JEALOUSY, RelevantTrait.ATTACHMENT, RelevantTrait.EXPRESSIVENESS);
        }
        if (containsAny(context, "전화", "call", "lead", "리드", "먼저")) {
            return EnumSet.of(RelevantTrait.DOMINANCE, RelevantTrait.CONFIDENCE, RelevantTrait.AFFECTION);
        }

        return switch (eventType) {
            case AFFECTION -> EnumSet.of(
                    RelevantTrait.AFFECTION,
                    RelevantTrait.ATTACHMENT,
                    RelevantTrait.EXPRESSIVENESS
            );
            case BREAKUP_DECLARATION, BREAKUP_RETRACTION, APOLOGY, INSULT -> EnumSet.of(
                    RelevantTrait.EMOTIONAL_STABILITY,
                    RelevantTrait.EXPRESSIVENESS,
                    RelevantTrait.EMPATHY
            );
            case IGNORE_OR_COLD -> EnumSet.of(
                    RelevantTrait.EMOTIONAL_STABILITY,
                    RelevantTrait.EXPRESSIVENESS,
                    RelevantTrait.EMPATHY
            );
            case NORMAL -> normalTraits(context);
        };
    }

    private Set<RelevantTrait> normalTraits(String context) {
        if (containsAny(context, "sad", "슬픔", "힘듦", "힘들", "고민", "우울", "불안")) {
            return EnumSet.of(RelevantTrait.EMPATHY, RelevantTrait.EMOTIONAL_STABILITY);
        }
        if (containsAny(context, "affection", "보고 싶", "사랑", "좋아", "애정")) {
            return EnumSet.of(RelevantTrait.AFFECTION, RelevantTrait.ATTACHMENT, RelevantTrait.EXPRESSIVENESS);
        }
        return EnumSet.of(RelevantTrait.HUMOR, RelevantTrait.PLAYFULNESS, RelevantTrait.CONFIDENCE);
    }

    private boolean isJealousyContext(String context) {
        return containsAny(context, "jealous", "질투", "다른 사람", "전 애인", "전남친", "전여친");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    public enum RelevantTrait {
        HUMOR,
        PLAYFULNESS,
        AFFECTION,
        EMPATHY,
        ATTACHMENT,
        JEALOUSY,
        DOMINANCE,
        CONFIDENCE,
        EXPRESSIVENESS,
        EMOTIONAL_STABILITY
    }
}
