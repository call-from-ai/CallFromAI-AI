package com.example.aidatingagentbackend.context;

import com.example.aidatingagentbackend.engine.EmotionEngine;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.engine.MemoryEngine;
import com.example.aidatingagentbackend.engine.RelationshipEngine;
import com.example.aidatingagentbackend.entity.Memory;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.entity.State;
import com.example.aidatingagentbackend.entity.TurningPoint;
import com.example.aidatingagentbackend.repository.MemoryRepository;
import com.example.aidatingagentbackend.repository.RelationshipRepository;
import com.example.aidatingagentbackend.repository.StateRepository;
import com.example.aidatingagentbackend.repository.TurningPointRepository;
import com.example.aidatingagentbackend.service.SettingsDefaultPolicy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContextUpdater {

    private final EmotionEngine emotionEngine;
    private final RelationshipEngine relationshipEngine;
    private final MemoryEngine memoryEngine;
    private final MemoryEmbeddingService memoryEmbeddingService;

    private final StateRepository stateRepository;
    private final RelationshipRepository relationshipRepository;
    private final MemoryRepository memoryRepository;
    private final TurningPointRepository turningPointRepository;
    private final SettingsDefaultPolicy settingsDefaultPolicy;

    private static final List<TurningPointRule> TURNING_POINT_RULES = List.of(
            new TurningPointRule("CONFESSION", 8, List.of("고백", "좋아해", "사랑해")),
            new TurningPointRule("FIRST_DATE", 7, List.of("첫 데이트", "처음 데이트")),
            new TurningPointRule("CONFLICT", 8, List.of("싸웠", "화났", "실망", "배신", "거짓말")),
            new TurningPointRule("REPAIR", 7, List.of("사과", "미안", "화해", "잘못했")),
            new TurningPointRule("ANNIVERSARY", 6, List.of("기념일", "100일", "1주년")),
            new TurningPointRule("BREAKUP_RISK", 10, List.of("헤어지자", "그만 만나", "끝내자", "이별"))
    );


        public ContextUpdater(
                EmotionEngine emotionEngine,
                RelationshipEngine relationshipEngine,
                MemoryEngine memoryEngine,
                MemoryEmbeddingService memoryEmbeddingService,
                StateRepository stateRepository,
                RelationshipRepository relationshipRepository,
                MemoryRepository memoryRepository,
                TurningPointRepository turningPointRepository,
                SettingsDefaultPolicy settingsDefaultPolicy
        ) {
            this.emotionEngine = emotionEngine;
            this.relationshipEngine = relationshipEngine;
            this.memoryEngine = memoryEngine;
            this.memoryEmbeddingService = memoryEmbeddingService;
            this.stateRepository = stateRepository;
            this.relationshipRepository = relationshipRepository;
            this.memoryRepository = memoryRepository;
            this.turningPointRepository = turningPointRepository;
            this.settingsDefaultPolicy = settingsDefaultPolicy;
        }


    public void updateBeforeResponse(Long characterId, String userMessage, EventAnalysis eventAnalysis){

        State state =
                stateRepository.findByCharacterId(characterId)
                        .orElseGet(() -> createDefaultState(characterId));

        Relationship relationship =
                relationshipRepository.findByCharacterId(characterId)
                        .orElseGet(() -> createDefaultRelationship(characterId));

        var emotion =
                emotionEngine.analyze(state,userMessage,eventAnalysis);

        var relation =
                relationshipEngine.analyze(relationship,userMessage,eventAnalysis);

        state.setEmotion(emotion.emotion());
        state.setEmotionIntensity(emotion.emotionIntensity());

        relationship.setTrust(relation.trust());
        relationship.setCloseness(relation.closeness());
        relationship.setConflictLevel(relation.conflictLevel());
        relationship.setRepairProgress(relation.repairProgress());
        relationship.setBreakupRisk(relation.breakupRisk());

        stateRepository.save(state);
        relationshipRepository.save(relationship);
    }

    public void updateMemoryAfterResponse(Long characterId, String userMessage,String reply){

        State state =
                stateRepository.findByCharacterId(characterId)
                        .orElseGet(() -> createDefaultState(characterId));

        var decision =
                memoryEngine.analyze(
                        userMessage+"\n"+reply,
                        state.getEmotion(),
                        state.getEmotionIntensity());

        if(Boolean.TRUE.equals(decision.shouldCreate())){

            Memory memory=new Memory();

            memory.setCharacterId(characterId);
            memory.setType(decision.memoryType());
            memory.setSummary(decision.episodeSummary());
            memory.setEmbedding(memoryEmbeddingService.serialize(memoryEmbeddingService.embed(decision.episodeSummary())));
            memory.setImportance(decision.importance());

            memoryRepository.save(memory);
        }

        saveTurningPointIfNeeded(characterId, userMessage, reply, state);

    }

    private State createDefaultState(Long characterId) {
        State state = new State();
        state.setCharacterId(characterId);
        state.setEmotion("neutral");
        state.setEmotionIntensity(0);
        state.setEnergy(50);
        state.setStress(20);
        return state;
    }

    private Relationship createDefaultRelationship(Long characterId) {
        Relationship relationship = new Relationship();
        relationship.setCharacterId(characterId);
        relationship.setTrust(50);
        relationship.setCloseness(30);
        relationship.setConflictLevel(0);
        relationship.setRepairProgress(0);
        relationship.setBreakupRisk(0);
        relationship.setRelationshipStage(settingsDefaultPolicy.defaultRelationshipStageValue());
        relationship.setRelationshipTemperatureScore(settingsDefaultPolicy.defaultRelationshipTemperatureScore());
        relationship.setDaysTogether(0);
        return relationship;
    }

    private void saveTurningPointIfNeeded(Long characterId, String userMessage, String reply, State state) {
        String conversation = ((userMessage == null ? "" : userMessage) + "\n" + (reply == null ? "" : reply)).toLowerCase();

        TURNING_POINT_RULES.stream()
                .filter(rule -> rule.matches(conversation))
                .findFirst()
                .ifPresent(rule -> {
                    TurningPoint turningPoint = new TurningPoint();
                    turningPoint.setCharacterId(characterId);
                    turningPoint.setEventType(rule.eventType());
                    turningPoint.setSummary(buildTurningPointSummary(userMessage, reply));
                    turningPoint.setImpactEmotion(state.getEmotion());
                    turningPoint.setImpactScore(rule.impactScore());
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

    private record TurningPointRule(String eventType, int impactScore, List<String> keywords) {

        boolean matches(String conversation) {
            return keywords.stream()
                    .map(String::toLowerCase)
                    .anyMatch(conversation::contains);
        }
    }
}
