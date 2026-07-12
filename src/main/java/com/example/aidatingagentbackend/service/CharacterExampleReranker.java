package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.entity.CharacterExample;
import com.example.aidatingagentbackend.dto.CharacterTraitSnapshot;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import com.example.aidatingagentbackend.entity.RelationshipTemperatureBand;
import com.example.aidatingagentbackend.entity.RomanceStyleBand;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CharacterExampleReranker {

    private static final int MAX_EXAMPLES = 5;

    private final CharacterExampleToneTagPolicy toneTagPolicy;
    private final RelationshipTemperatureScoreResolver temperatureScoreResolver;
    private final CharacterExampleRelevantTraitPolicy relevantTraitPolicy;

    public CharacterExampleReranker(
            CharacterExampleToneTagPolicy toneTagPolicy,
            RelationshipTemperatureScoreResolver temperatureScoreResolver,
            CharacterExampleRelevantTraitPolicy relevantTraitPolicy
    ) {
        this.toneTagPolicy = toneTagPolicy;
        this.temperatureScoreResolver = temperatureScoreResolver;
        this.relevantTraitPolicy = relevantTraitPolicy;
    }

    public List<CharacterExample> rerank(
            List<CharacterExample> candidates,
            EventAnalysis eventAnalysis,
            RelationshipStage stage,
            Integer relationshipTemperatureScore,
            Integer romanceStyleScore,
            CharacterTraitSnapshot traits
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        boolean jealousyContext = isJealousyContext(eventAnalysis);
        RelationshipTemperatureBand band = temperatureScoreResolver.resolveBand(relationshipTemperatureScore);
        RomanceStyleBand romanceBand = RomanceStyleBand.from(romanceStyleScore == null ? 50 : Math.max(0, Math.min(100, romanceStyleScore)));
        Set<CharacterExampleRelevantTraitPolicy.RelevantTrait> relevantTraits = relevantTraitPolicy.select(eventAnalysis);
        List<ScoredExample> scored = candidates.stream()
                .filter(example -> isActive(example)
                        && matchesStage(example, stage)
                        && matchesTemperature(example, relationshipTemperatureScore)
                        && !forbiddenByStage(example, stage)
                        && !forbiddenByTemperature(example, relationshipTemperatureScore, jealousyContext))
                .map(example -> new ScoredExample(
                        example,
                        score(example, eventAnalysis, stage, band, romanceBand, traits, jealousyContext, relevantTraits)
                ))
                .sorted(Comparator.comparingDouble(ScoredExample::score).reversed()
                        .thenComparing(example -> example.example().getId(), Comparator.nullsLast(Long::compareTo)))
                .toList();

        return selectWithDiversity(scored);
    }

    private double score(
            CharacterExample example,
            EventAnalysis eventAnalysis,
            RelationshipStage stage,
            RelationshipTemperatureBand band,
            RomanceStyleBand romanceStyleBand,
            CharacterTraitSnapshot traits,
            boolean jealousyContext,
            Set<CharacterExampleRelevantTraitPolicy.RelevantTrait> relevantTraits
    ) {
        double score = value(example.getPriority()) * 0.4;
        score += eventScore(example, eventAnalysis);
        score += stageScore(example, stage);
        score += toneTagPolicy.score(example.getToneTag(), traits, band, jealousyContext, relevantTraits);
        if (example.getRomanceStyleBand() == romanceStyleBand) score += 6.0;
        return score;
    }

    private double eventScore(CharacterExample example, EventAnalysis eventAnalysis) {
        AgentEventType eventType = eventAnalysis == null ? AgentEventType.NORMAL : eventAnalysis.eventType();
        if (example.getEventType() == eventType) {
            return 30.0;
        }
        if (example.getEventType() == null) {
            return 8.0;
        }
        return 0.0;
    }

    private double stageScore(CharacterExample example, RelationshipStage stage) {
        if (example.getRelationshipStage() == null || example.getRelationshipStage().isBlank()) {
            return 4.0;
        }
        return example.getRelationshipStage().equalsIgnoreCase((stage == null ? RelationshipStage.CRUSH : stage).name())
                ? 12.0
                : 0.0;
    }

    private List<CharacterExample> selectWithDiversity(List<ScoredExample> scored) {
        List<CharacterExample> selected = new ArrayList<>();
        Set<String> seenText = new HashSet<>();
        Set<String> usedFamilies = new HashSet<>();

        for (ScoredExample scoredExample : scored) {
            CharacterExample example = scoredExample.example();
            String textKey = textKey(example);
            if (!seenText.add(textKey)) {
                continue;
            }
            String family = toneTagPolicy.family(example.getToneTag());
            if (selected.size() >= 2 && usedFamilies.contains(family) && hasAlternativeFamily(scored, seenText, usedFamilies)) {
                continue;
            }
            usedFamilies.add(family);
            selected.add(example);
            if (selected.size() == MAX_EXAMPLES) {
                return selected;
            }
        }

        if (selected.size() < MAX_EXAMPLES) {
            for (ScoredExample scoredExample : scored) {
                CharacterExample example = scoredExample.example();
                String textKey = textKey(example);
                if (selected.contains(example) || !seenText.add(textKey)) {
                    continue;
                }
                selected.add(example);
                if (selected.size() == MAX_EXAMPLES) {
                    break;
                }
            }
        }
        return selected;
    }

    private boolean hasAlternativeFamily(
            List<ScoredExample> scored,
            Set<String> seenText,
            Set<String> usedFamilies
    ) {
        return scored.stream()
                .map(ScoredExample::example)
                .anyMatch(example -> {
                    String textKey = textKey(example);
                    return !seenText.contains(textKey) && !usedFamilies.contains(toneTagPolicy.family(example.getToneTag()));
                });
    }

    private boolean matchesStage(CharacterExample example, RelationshipStage stage) {
        if (example.getRelationshipStage() == null || example.getRelationshipStage().isBlank()) {
            return true;
        }
        return example.getRelationshipStage().equalsIgnoreCase((stage == null ? RelationshipStage.CRUSH : stage).name());
    }

    private boolean matchesTemperature(CharacterExample example, Integer temperatureScore) {
        int score = temperatureScoreResolver.resolveScore(temperatureScore);
        Integer min = example.getMinTemperatureScore();
        Integer max = example.getMaxTemperatureScore();
        return (min == null || score >= min) && (max == null || score <= max);
    }

    private boolean forbiddenByStage(CharacterExample example, RelationshipStage stage) {
        RelationshipStage resolvedStage = stage == null ? RelationshipStage.CRUSH : stage;
        return resolvedStage == RelationshipStage.CRUSH
                && toneTagPolicy.isStrongPossessive(example.getToneTag(), example.getAssistantExample());
    }

    private boolean forbiddenByTemperature(CharacterExample example, Integer temperatureScore, boolean jealousyContext) {
        int score = temperatureScoreResolver.resolveScore(temperatureScore);
        return score >= 81 && toneTagPolicy.isJealousTone(example.getToneTag()) && !jealousyContext;
    }

    private boolean isActive(CharacterExample example) {
        return example.getActive() == null || Boolean.TRUE.equals(example.getActive());
    }

    private boolean isJealousyContext(EventAnalysis eventAnalysis) {
        if (eventAnalysis == null) {
            return false;
        }
        String emotion = normalize(eventAnalysis.primaryEmotion());
        String summary = normalize(eventAnalysis.summary());
        return emotion.contains("jealous")
                || emotion.contains("질투")
                || summary.contains("질투")
                || summary.contains("다른 사람")
                || summary.contains("전 애인");
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase();
    }

    private String textKey(CharacterExample example) {
        return normalize(example.getAssistantExample());
    }

    private record ScoredExample(CharacterExample example, double score) {
    }
}

