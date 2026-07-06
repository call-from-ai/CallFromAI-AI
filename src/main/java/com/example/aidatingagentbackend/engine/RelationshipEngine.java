package com.example.aidatingagentbackend.engine;

import com.example.aidatingagentbackend.entity.Relationship;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RelationshipEngine {

    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 100;

    private static final List<RelationshipRule> RULES = List.of(
            new RelationshipRule(-25, -20, 25, -15, 35, List.of("헤어지자", "그만 만나", "끝내자", "이별")),
            new RelationshipRule(-30, -15, 30, -20, 25, List.of("배신", "거짓말", "속였", "믿었는데")),
            new RelationshipRule(8, 4, -8, 20, -10, List.of("사과", "미안", "죄송", "잘못했")),
            new RelationshipRule(4, 8, -3, 6, -4, List.of("칭찬", "멋져", "예뻐", "고마워", "좋아해")),
            new RelationshipRule(-8, -6, 10, -6, 8, List.of("질투", "다른 사람", "전 애인", "전남친", "전여친")),
            new RelationshipRule(-6, -4, 8, -4, 6, List.of("답장 늦음", "답장이 늦", "왜 늦게", "읽씹", "안읽씹")),
            new RelationshipRule(6, 10, -4, 5, -4, List.of("데이트", "같이", "함께", "보고 싶", "만나고 싶")),
            new RelationshipRule(10, 6, -6, 8, -8, List.of("약속 지켰", "믿어", "솔직", "진심"))
    );

    public RelationshipResult analyze(Relationship currentRelationship, String event) {
        int currentTrust = resolveTrust(currentRelationship);
        int currentCloseness = resolveCloseness(currentRelationship);
        int currentConflictLevel = resolveConflictLevel(currentRelationship);
        int currentRepairProgress = resolveRepairProgress(currentRelationship);
        int currentBreakupRisk = resolveBreakupRisk(currentRelationship);

        if (event == null || event.isBlank()) {
            return new RelationshipResult(
                    currentTrust,
                    currentCloseness,
                    currentConflictLevel,
                    currentRepairProgress,
                    currentBreakupRisk
            );
        }

        String normalizedEvent = event.toLowerCase();
        RelationshipRule matchedRule = findMatchedRule(normalizedEvent);
        if (matchedRule == null) {
            return new RelationshipResult(
                    currentTrust,
                    currentCloseness,
                    currentConflictLevel,
                    currentRepairProgress,
                    currentBreakupRisk
            );
        }

        int nextTrust = clamp(currentTrust + matchedRule.trustDelta());
        int nextCloseness = clamp(currentCloseness + matchedRule.closenessDelta());
        int nextConflictLevel = clamp(currentConflictLevel + matchedRule.conflictDelta());
        int nextRepairProgress = clamp(currentRepairProgress + matchedRule.repairDelta());
        int nextBreakupRisk = clamp(currentBreakupRisk + matchedRule.breakupRiskDelta());
        return new RelationshipResult(
                nextTrust,
                nextCloseness,
                nextConflictLevel,
                nextRepairProgress,
                nextBreakupRisk
        );
    }

    private RelationshipRule findMatchedRule(String event) {
        return RULES.stream()
                .filter(rule -> rule.matches(event))
                .findFirst()
                .orElse(null);
    }

    private int resolveTrust(Relationship relationship) {
        if (relationship == null || relationship.getTrust() == null) {
            return MIN_SCORE;
        }

        return clamp(relationship.getTrust());
    }

    private int resolveCloseness(Relationship relationship) {
        if (relationship == null || relationship.getCloseness() == null) {
            return MIN_SCORE;
        }

        return clamp(relationship.getCloseness());
    }

    private int resolveConflictLevel(Relationship relationship) {
        if (relationship == null || relationship.getConflictLevel() == null) {
            return MIN_SCORE;
        }

        return clamp(relationship.getConflictLevel());
    }

    private int resolveRepairProgress(Relationship relationship) {
        if (relationship == null || relationship.getRepairProgress() == null) {
            return MIN_SCORE;
        }

        return clamp(relationship.getRepairProgress());
    }

    private int resolveBreakupRisk(Relationship relationship) {
        if (relationship == null || relationship.getBreakupRisk() == null) {
            return MIN_SCORE;
        }

        return clamp(relationship.getBreakupRisk());
    }

    private int clamp(int value) {
        return Math.max(MIN_SCORE, Math.min(MAX_SCORE, value));
    }

    public record RelationshipResult(
            Integer trust,
            Integer closeness,
            Integer conflictLevel,
            Integer repairProgress,
            Integer breakupRisk
    ) {
    }

    private record RelationshipRule(
            int trustDelta,
            int closenessDelta,
            int conflictDelta,
            int repairDelta,
            int breakupRiskDelta,
            List<String> keywords
    ) {

        boolean matches(String event) {
            return keywords.stream()
                    .map(String::toLowerCase)
                    .anyMatch(event::contains);
        }
    }
}
