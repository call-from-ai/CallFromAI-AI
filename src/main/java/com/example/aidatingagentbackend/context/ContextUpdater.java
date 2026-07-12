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

        var relation =
                relationshipEngine.analyze(relationship,userMessage,eventAnalysis);
        RelationshipSnapshot next = new RelationshipSnapshot(
                relationship.relationshipId(), relationship.relationshipStage(), relationship.relationshipTemperatureScore(),
                relation.trust(), relation.closeness(), relation.conflictLevel(), relation.repairProgress(), relation.breakupRisk(),
                relationship.daysTogether(), relationship.strategy());
        RelationshipDelta delta = new RelationshipDelta(
                relation.trust() - relationship.trust(), relation.closeness() - relationship.closeness(),
                relation.conflictLevel() - relationship.conflictLevel(), relation.repairProgress() - relationship.repairProgress(),
                relation.breakupRisk() - relationship.breakupRisk());
        return new RelationshipUpdate(delta, next);
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
