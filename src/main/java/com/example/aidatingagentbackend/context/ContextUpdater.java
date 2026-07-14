package com.example.aidatingagentbackend.context;

import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.engine.MemoryEngine;
import com.example.aidatingagentbackend.engine.RelationshipEngine;
import com.example.aidatingagentbackend.dto.RelationshipDelta;
import com.example.aidatingagentbackend.dto.RelationshipSnapshot;
import com.example.aidatingagentbackend.entity.Memory;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.TurningPoint;
import com.example.aidatingagentbackend.repository.MemoryRepository;
import com.example.aidatingagentbackend.repository.AgentSelfStateRepository;
import com.example.aidatingagentbackend.repository.TurningPointRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContextUpdater {

    private final RelationshipEngine relationshipEngine;
    private final MemoryEngine memoryEngine;
    private final MemoryEmbeddingService memoryEmbeddingService;

    private final AgentSelfStateRepository agentSelfStateRepository;
    private final MemoryRepository memoryRepository;
    private final TurningPointRepository turningPointRepository;

    private static final List<TurningPointRule> TURNING_POINT_RULES = List.of(
            new TurningPointRule("CONFESSION", List.of("고백", "좋아해", "사랑해")),
            new TurningPointRule("FIRST_DATE", List.of("첫 데이트", "처음 데이트")),
            new TurningPointRule("CONFLICT", List.of("싸웠", "화났", "실망", "배신", "거짓말")),
            new TurningPointRule("REPAIR", List.of("사과", "미안", "화해", "잘못했")),
            new TurningPointRule("ANNIVERSARY", List.of("기념일", "100일", "1주년")),
            new TurningPointRule("BREAKUP_RISK", List.of("헤어지자", "그만 만나", "끝내자", "이별"))
    );


        public ContextUpdater(
                RelationshipEngine relationshipEngine,
                MemoryEngine memoryEngine,
                MemoryEmbeddingService memoryEmbeddingService,
                AgentSelfStateRepository agentSelfStateRepository,
                MemoryRepository memoryRepository,
                TurningPointRepository turningPointRepository
        ) {
            this.relationshipEngine = relationshipEngine;
            this.memoryEngine = memoryEngine;
            this.memoryEmbeddingService = memoryEmbeddingService;
            this.agentSelfStateRepository = agentSelfStateRepository;
            this.memoryRepository = memoryRepository;
            this.turningPointRepository = turningPointRepository;
        }


    public RelationshipUpdate updateBeforeResponse(Long characterId, RelationshipSnapshot relationship, String userMessage, EventAnalysis eventAnalysis){
        RelationshipSnapshot resolvedRelationship = resolveInternalRelationship(characterId, relationship);
        var relation =
                relationshipEngine.analyze(resolvedRelationship,userMessage,eventAnalysis);
        RelationshipSnapshot next = new RelationshipSnapshot(
                relationship.relationshipId(), relationship.relationshipStage(), relationship.relationshipTemperatureScore(),
                relation.trust(), relation.closeness(), relation.conflictLevel(), relation.repairProgress(), relation.breakupRisk(),
                relationship.daysTogether(), relationship.strategy());
        RelationshipDelta delta = new RelationshipDelta(
                relation.trust() - resolvedRelationship.trust(), relation.closeness() - resolvedRelationship.closeness(),
                relation.conflictLevel() - resolvedRelationship.conflictLevel(), relation.repairProgress() - resolvedRelationship.repairProgress(),
                relation.breakupRisk() - resolvedRelationship.breakupRisk());
        return new RelationshipUpdate(delta, next);
    }

    private RelationshipSnapshot resolveInternalRelationship(Long characterId, RelationshipSnapshot request) {
        AgentSelfState state = agentSelfStateRepository.findByCharacterId(characterId).orElse(null);
        int closeness = state == null ? 50 : closenessFrom(state);
        int conflictLevel = state == null ? 0 : conflictFrom(state);
        return new RelationshipSnapshot(request.relationshipId(), request.relationshipStage(),
                request.relationshipTemperatureScore(), request.trust(), closeness, conflictLevel,
                request.repairProgress(), request.breakupRisk(), request.daysTogether(), request.strategy());
    }

    private int closenessFrom(AgentSelfState state) {
        double affection = state.getAffection() == null ? 0.55 : state.getAffection();
        double trust = state.getTrust() == null ? 0.60 : state.getTrust();
        double distance = state.getEmotionalDistance() == null ? 0.15 : state.getEmotionalDistance();
        return score(((affection + trust) / 2.0) * (1.0 - distance));
    }

    private int conflictFrom(AgentSelfState state) {
        double hurt = state.getHurt() == null ? 0.0 : state.getHurt();
        double anger = state.getAnger() == null ? 0.0 : state.getAnger();
        double disappointment = state.getDisappointment() == null ? 0.0 : state.getDisappointment();
        return score((hurt + anger + disappointment) / 3.0);
    }

    private int score(double value) {
        return Math.max(0, Math.min(100, (int) Math.round(value * 100.0)));
    }

    public void updateMemoryAfterResponse(Long characterId, String userMessage,String reply){

        AgentSelfState selfState = agentSelfStateRepository.findByCharacterId(characterId).orElse(null);
        String emotion = selfState == null ? "calm" : selfState.representativeEmotion();
        int emotionIntensity = selfState == null ? 0 : selfState.emotionIntensity();

        var decision =
                memoryEngine.analyze(
                        userMessage+"\n"+reply,
                        emotion,
                        emotionIntensity);

        if(Boolean.TRUE.equals(decision.shouldCreate())){

            Memory memory=new Memory();

            memory.setCharacterId(characterId);
            memory.setType(decision.memoryType());
            memory.setSummary(decision.episodeSummary());
            memory.setEmbedding(memoryEmbeddingService.serialize(memoryEmbeddingService.embed(decision.episodeSummary())));
            memory.setImportance(decision.importance());

            memoryRepository.save(memory);
        }

        saveTurningPointIfNeeded(characterId, userMessage, reply, emotion, emotionIntensity);

    }

    private void saveTurningPointIfNeeded(Long characterId, String userMessage, String reply,
                                          String emotion, int emotionIntensity) {
        String conversation = ((userMessage == null ? "" : userMessage) + "\n" + (reply == null ? "" : reply)).toLowerCase();

        TURNING_POINT_RULES.stream()
                .filter(rule -> rule.matches(conversation))
                .findFirst()
                .ifPresent(rule -> {
                    TurningPoint turningPoint = new TurningPoint();
                    turningPoint.setCharacterId(characterId);
                    turningPoint.setEventType(rule.eventType());
                    turningPoint.setSummary(buildTurningPointSummary(userMessage, reply));
                    turningPoint.setImpactEmotion(emotion);
                    turningPoint.setImpactScore(emotionIntensity);
                    turningPoint.setCreatedAt(LocalDateTime.now());
                    turningPointRepository.save(turningPoint);
                });
    }

    private String buildTurningPointSummary(String userMessage, String reply) {
        String summary = "User: " + nullToBlank(userMessage) + "\nAssistant: " + nullToBlank(reply);
        if (summary.length() > 300) {
            return summary.substring(0, 300).strip() + "...";
        }

        return summary;
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private record TurningPointRule(String eventType, List<String> keywords) {

        boolean matches(String conversation) {
            return keywords.stream()
                    .map(String::toLowerCase)
                    .anyMatch(conversation::contains);
        }
    }

    public record RelationshipUpdate(RelationshipDelta delta, RelationshipSnapshot nextRelationship) {}
}
