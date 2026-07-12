package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.domain.EmotionDelta;
import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.engine.EventAnalyzer;
import com.example.aidatingagentbackend.engine.EventDetector;
import com.example.aidatingagentbackend.engine.MessageSignalDetector;
import com.example.aidatingagentbackend.engine.MessageSignalType;
import com.example.aidatingagentbackend.engine.MessageSignals;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.AgentSelfStateLog;
import com.example.aidatingagentbackend.dto.AgentSelfStateSnapshot;
import com.example.aidatingagentbackend.dto.ChatHistoryItem;
import com.example.aidatingagentbackend.dto.CharacterTraitSnapshot;
import com.example.aidatingagentbackend.dto.RelationshipSnapshot;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import com.example.aidatingagentbackend.repository.AgentSelfStateLogRepository;
import com.example.aidatingagentbackend.repository.AgentSelfStateRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmotionUpdateService {

    private static final double MIN_VALUE = 0.0;
    private static final double MAX_VALUE = 1.0;
    private static final String BREAKUP_EVENT = "user_declared_breakup";
    private static final int MAX_OPTIMISTIC_RETRIES = 1;

    private final AgentSelfStateRepository agentSelfStateRepository;
    private final EventAnalyzer eventAnalyzer;
    private final EventDetector eventDetector;
    private final AgentSelfStateLogRepository agentSelfStateLogRepository;
    private final MessageSignalDetector messageSignalDetector;
    private final EmotionTraitModifier emotionTraitModifier;
    private final RelationshipStageEmotionPolicy relationshipStageEmotionPolicy;

    public EmotionUpdateService(
            AgentSelfStateRepository agentSelfStateRepository,
            EventAnalyzer eventAnalyzer,
            EventDetector eventDetector,
            AgentSelfStateLogRepository agentSelfStateLogRepository,
            MessageSignalDetector messageSignalDetector,
            EmotionTraitModifier emotionTraitModifier,
            RelationshipStageEmotionPolicy relationshipStageEmotionPolicy
    ) {
        this.agentSelfStateRepository = agentSelfStateRepository;
        this.eventAnalyzer = eventAnalyzer;
        this.eventDetector = eventDetector;
        this.agentSelfStateLogRepository = agentSelfStateLogRepository;
        this.messageSignalDetector = messageSignalDetector;
        this.emotionTraitModifier = emotionTraitModifier;
        this.relationshipStageEmotionPolicy = relationshipStageEmotionPolicy;
    }

    public EmotionUpdateService(
            AgentSelfStateRepository agentSelfStateRepository,
            EventAnalyzer eventAnalyzer,
            EventDetector eventDetector,
            AgentSelfStateLogRepository agentSelfStateLogRepository,
            MessageSignalDetector messageSignalDetector
    ) {
        this(
                agentSelfStateRepository,
                eventAnalyzer,
                eventDetector,
                agentSelfStateLogRepository,
                messageSignalDetector,
                new EmotionTraitModifier(),
                new RelationshipStageEmotionPolicy()
        );
    }

    public EmotionUpdateResult updateBeforeResponse(Long characterId, String userMessage, List<ChatHistoryItem> history,
                                                     CharacterTraitSnapshot traitProfile, RelationshipSnapshot relationship) {
        OptimisticLockingFailureException lastException = null;
        for (int attempt = 0; attempt <= MAX_OPTIMISTIC_RETRIES; attempt++) {
            try {
                return updateBeforeResponseOnce(characterId, userMessage, history, traitProfile, relationship);
            } catch (OptimisticLockingFailureException exception) {
                lastException = exception;
            }
        }
        throw lastException;
    }

    @Transactional
    public EmotionUpdateResult updateBeforeResponseOnce(Long characterId, String userMessage, List<ChatHistoryItem> history,
                                                         CharacterTraitSnapshot traitProfile, RelationshipSnapshot relationship) {
        AgentSelfState state = agentSelfStateRepository.findByCharacterId(characterId)
                .orElseGet(() -> createDefaultState(characterId));
        RelationshipStage relationshipStage = relationship.relationshipStage();
        AgentSelfStateSnapshot previousState = AgentSelfStateSnapshot.from(state);

        SelfStateSnapshot previousSnapshot = SelfStateSnapshot.from(state);
        LocalDateTime now = LocalDateTime.now();
        applyDecay(state, now, traitProfile);
        EventAnalysis eventAnalysis = analyzeEvent(userMessage, history, state);
        MessageSignals signals = messageSignalDetector.detect(userMessage);
        applyEvent(state, eventAnalysis, signals, traitProfile, relationshipStage);
        applyConversationTransition(state, signals, eventAnalysis, traitProfile, relationshipStage);
        normalize(state);
        saveLog(characterId, eventAnalysis, previousSnapshot, state, traitProfile, relationshipStage);

        AgentSelfState saved = agentSelfStateRepository.saveAndFlush(state);
        return new EmotionUpdateResult(previousState, AgentSelfStateSnapshot.from(saved), eventAnalysis);
    }

    @Transactional(readOnly = true)
    public AgentSelfState findOrCreatePreview(Long characterId) {
        return agentSelfStateRepository.findByCharacterId(characterId)
                .orElseGet(() -> createDefaultState(characterId));
    }

    private EventAnalysis analyzeEvent(String userMessage, List<ChatHistoryItem> history, AgentSelfState state) {
        if (eventAnalyzer == null) {
            return EventAnalysis.fallback(eventDetector.detect(userMessage));
        }

        return eventAnalyzer.analyze(
                userMessage,
                history == null ? List.of() : history,
                state
        );
    }

    private AgentSelfStateLog saveLog(
            Long userId,
            EventAnalysis eventAnalysis,
            SelfStateSnapshot previousSnapshot,
            AgentSelfState nextState,
            CharacterTraitSnapshot traitProfile,
            RelationshipStage relationshipStage
    ) {
        if (agentSelfStateLogRepository == null) {
            return null;
        }

        AgentSelfStateLog log = new AgentSelfStateLog();
        log.setUserId(userId);
        log.setPreviousHurt(previousSnapshot.hurt());
        log.setNextHurt(value(nextState.getHurt()));
        log.setPreviousTrust(previousSnapshot.trust());
        log.setNextTrust(value(nextState.getTrust()));
        log.setPreviousAnger(previousSnapshot.anger());
        log.setNextAnger(value(nextState.getAnger()));
        log.setPreviousInsecurity(previousSnapshot.insecurity());
        log.setNextInsecurity(value(nextState.getInsecurity()));
        log.setEventType(eventAnalysis.eventType().name());
        log.setSeverity(eventAnalysis.severity());
        log.setUserMessage(null);
        log.setDeltaReason(buildDeltaReason(eventAnalysis, previousSnapshot, nextState, traitProfile, relationshipStage));
        return agentSelfStateLogRepository.save(log);
    }

    private String buildDeltaReason(
            EventAnalysis eventAnalysis,
            SelfStateSnapshot previousSnapshot,
            AgentSelfState nextState,
            CharacterTraitSnapshot traitProfile,
            RelationshipStage relationshipStage
    ) {
        EmotionDelta baseDelta = baseEventDelta(nextState, eventAnalysis);
        double finalHurtDelta = value(nextState.getHurt()) - value(previousSnapshot.hurt());
        double finalAngerDelta = value(nextState.getAnger()) - value(previousSnapshot.anger());
        double finalInsecurityDelta = value(nextState.getInsecurity()) - value(previousSnapshot.insecurity());
        StringBuilder reason = new StringBuilder();
        reason.append("eventType=").append(eventAnalysis.eventType());
        reason.append(", severity=").append(eventAnalysis.severity());
        reason.append(", sincerity=").append(eventAnalysis.sincerity());
        reason.append(", isJoke=").append(eventAnalysis.isJoke());
        reason.append(", isManipulative=").append(eventAnalysis.isManipulative());
        reason.append(", primaryEmotion=").append(eventAnalysis.primaryEmotion());
        reason.append(", stage=").append(relationshipStage);
        reason.append(", traits={attachment=").append(traitValue(traitProfile == null ? null : traitProfile.getAttachment()));
        reason.append(", jealousy=").append(traitValue(traitProfile == null ? null : traitProfile.getJealousy()));
        reason.append(", affection=").append(traitValue(traitProfile == null ? null : traitProfile.getAffection()));
        reason.append(", emotionalStability=").append(traitValue(traitProfile == null ? null : traitProfile.getEmotionalStability()));
        reason.append("}");
        reason.append(", baseDelta={hurt=").append(baseDelta.hurt());
        reason.append(", anger=").append(baseDelta.anger());
        reason.append(", insecurity=").append(baseDelta.insecurity()).append("}");
        reason.append(", finalDelta={hurt=").append(finalHurtDelta);
        reason.append(", anger=").append(finalAngerDelta);
        reason.append(", insecurity=").append(finalInsecurityDelta).append("}");
        return reason.toString();
    }

    public void applyDecay(AgentSelfState state, LocalDateTime now) {
        applyDecay(state, now, null);
    }

    public void applyDecay(AgentSelfState state, LocalDateTime now, CharacterTraitSnapshot traitProfile) {
        if (state.getUpdatedAt() == null || now == null || !now.isAfter(state.getUpdatedAt())) {
            return;
        }

        long hours = Duration.between(state.getUpdatedAt(), now).toHours();
        if (hours <= 0) {
            return;
        }

        double decay = Math.min(0.25, hours * 0.01 * emotionTraitModifierOrDefault().decayModifier(traitProfile));
        state.setHurt(value(state.getHurt()) - decay);
        state.setAnger(value(state.getAnger()) - decay);
        state.setInsecurity(value(state.getInsecurity()) - decay * 0.8);
        state.setDisappointment(value(state.getDisappointment()) - decay * 0.7);
        state.setEmotionalDistance(value(state.getEmotionalDistance()) - decay * 0.4);
    }

    public void applyEvent(AgentSelfState state, AgentEventType eventType) {
        applyDelta(state, baseEventDelta(state, EventAnalysis.fallback(eventType)));
        applyEventSideEffects(state, eventType);
    }

    private void applyEvent(
            AgentSelfState state,
            EventAnalysis eventAnalysis,
            MessageSignals signals,
            CharacterTraitSnapshot traitProfile,
            RelationshipStage relationshipStage
    ) {
        EmotionDelta baseDelta = baseEventDelta(state, eventAnalysis);
        EmotionDelta traitDelta = emotionTraitModifier.apply(baseDelta, traitProfile, eventAnalysis, signals);
        EmotionDelta finalDelta = relationshipStageEmotionPolicy.apply(traitDelta, relationshipStage, eventAnalysis, signals);
        applyDelta(state, finalDelta);
        applyEventSideEffects(state, eventAnalysis.eventType());
    }

    private void applyConversationTransition(
            AgentSelfState state,
            MessageSignals signals,
            EventAnalysis eventAnalysis
    ) {
        applyConversationTransition(state, signals, eventAnalysis, null, RelationshipStage.CRUSH);
    }

    private void applyConversationTransition(
            AgentSelfState state,
            MessageSignals signals,
            EventAnalysis eventAnalysis,
            CharacterTraitSnapshot traitProfile,
            RelationshipStage relationshipStage
    ) {
        if (signals.hasAny(MessageSignalType.AFFECTION, MessageSignalType.USER_RETURNED_TO_TALK)) {
            applyModifiedDelta(
                    state,
                    new EmotionDelta(0.08, 0.06, -0.12, -0.08, -0.08, 0.0, -0.08),
                    eventAnalysis,
                    signals,
                    traitProfile,
                    relationshipStage
            );
            state.setLastEmotion(value(state.getHurt()) > 0.45 ? "guarded_but_softening" : "softened");
            state.setLastSignificantEvent("user_returned_to_talk");
            return;
        }

        if (signals.has(MessageSignalType.APOLOGY)) {
            applyModifiedDelta(
                    state,
                    new EmotionDelta(0.0, 0.04, -0.12, -0.1, 0.0, 0.0, 0.0),
                    eventAnalysis,
                    signals,
                    traitProfile,
                    relationshipStage
            );
            state.setLastEmotion(value(state.getHurt()) > 0.45 ? "hurt_but_listening" : "softened");
            return;
        }

        if (signals.has(MessageSignalType.ASK_AGENT_SELF_DISCLOSURE)) {
            applyModifiedDelta(
                    state,
                    new EmotionDelta(0.0, 0.0, -0.06, -0.04, 0.0, 0.0, 0.0),
                    eventAnalysis,
                    signals,
                    traitProfile,
                    relationshipStage
            );
            state.setLastEmotion(value(state.getHurt()) > 0.5 ? "guarded_but_talking" : "curious");
            return;
        }

        if (signals.has(MessageSignalType.USER_SKIPPED_MEAL)) {
            applyModifiedDelta(
                    state,
                    new EmotionDelta(0.0, 0.0, 0.0, 0.0, 0.04, 0.0, 0.0),
                    eventAnalysis,
                    signals,
                    traitProfile,
                    relationshipStage
            );
            state.setLastEmotion("concerned");
            state.setLastSignificantEvent("user_skipped_meal");
            return;
        }

        if (eventAnalysis != null
                && eventAnalysis.eventType() == AgentEventType.NORMAL
                && signals.hasAny(
                MessageSignalType.CLUB,
                MessageSignalType.DEVELOPMENT,
                MessageSignalType.ASSIGNMENT_OR_CLASS,
                MessageSignalType.WORK_OR_BUSY
        )) {
            applyModifiedDelta(
                    state,
                    new EmotionDelta(0.0, 0.0, -0.04, 0.0, 0.0, 0.0, 0.0),
                    eventAnalysis,
                    signals,
                    traitProfile,
                    relationshipStage
            );
            state.setLastEmotion(value(state.getHurt()) > 0.5 ? "guarded_but_interested" : "interested");
        }
    }

    private void applyModifiedDelta(
            AgentSelfState state,
            EmotionDelta baseDelta,
            EventAnalysis eventAnalysis,
            MessageSignals signals,
            CharacterTraitSnapshot traitProfile,
            RelationshipStage relationshipStage
    ) {
        EmotionDelta traitDelta = emotionTraitModifier.apply(baseDelta, traitProfile, eventAnalysis, signals);
        EmotionDelta finalDelta = relationshipStageEmotionPolicy.apply(traitDelta, relationshipStage, eventAnalysis, signals);
        applyDelta(state, finalDelta);
    }

    private AgentSelfState createDefaultState(Long characterId) {
        AgentSelfState state = new AgentSelfState();
        state.setCharacterId(characterId);
        state.setAffection(0.55);
        state.setTrust(0.6);
        state.setHurt(0.0);
        state.setAnger(0.0);
        state.setInsecurity(0.15);
        state.setDisappointment(0.0);
        state.setEmotionalDistance(0.15);
        state.setLastEmotion("calm");
        state.setLastSignificantEvent("none");
        state.setUpdatedAt(LocalDateTime.now());
        return state;
    }

    private EmotionDelta baseEventDelta(AgentSelfState state, EventAnalysis eventAnalysis) {
        AgentEventType eventType = eventAnalysis == null ? AgentEventType.NORMAL : eventAnalysis.eventType();
        return switch (eventType) {
            case BREAKUP_DECLARATION -> new EmotionDelta(0.0, -0.3, 0.7, 0.35, 0.6, 0.45, 0.4);
            case BREAKUP_RETRACTION -> new EmotionDelta(0.0, 0.0, -0.1, -0.05, -0.1, 0.0, 0.0);
            case APOLOGY -> new EmotionDelta(0.0, 0.05, -0.2, -0.15, -0.08, -0.1, 0.0);
            case AFFECTION -> new EmotionDelta(0.08, 0.03, 0.0, 0.0, -0.04, 0.0, -0.04);
            case INSULT -> new EmotionDelta(0.0, -0.15, 0.35, 0.3, 0.0, 0.25, 0.2);
            case IGNORE_OR_COLD -> new EmotionDelta(0.0, 0.0, 0.0, 0.0, 0.2, 0.15, 0.15);
            case NORMAL -> EmotionDelta.none();
        };
    }

    private void applyDelta(AgentSelfState state, EmotionDelta delta) {
        state.setAffection(value(state.getAffection()) + delta.affection());
        state.setTrust(value(state.getTrust()) + delta.trust());
        state.setHurt(value(state.getHurt()) + delta.hurt());
        state.setAnger(value(state.getAnger()) + delta.anger());
        state.setInsecurity(value(state.getInsecurity()) + delta.insecurity());
        state.setDisappointment(value(state.getDisappointment()) + delta.disappointment());
        state.setEmotionalDistance(value(state.getEmotionalDistance()) + delta.emotionalDistance());
    }

    private void applyEventSideEffects(AgentSelfState state, AgentEventType eventType) {
        switch (eventType) {
            case BREAKUP_DECLARATION -> {
                state.setLastEmotion("hurt");
                state.setLastSignificantEvent(BREAKUP_EVENT);
            }
            case BREAKUP_RETRACTION -> applyBreakupRetractionFloor(state);
            case APOLOGY -> applyApologyFloor(state);
            case AFFECTION -> state.setLastEmotion(value(state.getHurt()) >= 0.5 ? "conflicted" : "affectionate");
            case INSULT -> {
                state.setLastEmotion("upset");
                state.setLastSignificantEvent("user_insulted_agent");
            }
            case IGNORE_OR_COLD -> {
                state.setLastEmotion("distant");
                state.setLastSignificantEvent("user_was_cold");
            }
            case NORMAL -> updateLastEmotion(state);
        }
    }

    private void applyBreakupRetractionFloor(AgentSelfState state) {
        boolean followsBreakup = BREAKUP_EVENT.equals(state.getLastSignificantEvent());
        if (followsBreakup) {
            state.setHurt(Math.max(value(state.getHurt()), 0.55));
            state.setAnger(Math.max(value(state.getAnger()), 0.25));
            state.setEmotionalDistance(Math.max(value(state.getEmotionalDistance()), 0.35));
            state.setLastSignificantEvent("user_retracted_breakup_after_hurting_agent");
        }

        if (value(state.getHurt()) >= 0.5) {
            state.setLastEmotion("hurt");
        } else if (value(state.getAnger()) >= 0.25) {
            state.setLastEmotion("upset");
        } else {
            state.setLastEmotion("guarded");
        }
    }

    private void applyApologyFloor(AgentSelfState state) {
        if (value(state.getHurt()) >= 0.45) {
            state.setHurt(Math.max(value(state.getHurt()), 0.35));
            state.setEmotionalDistance(Math.max(value(state.getEmotionalDistance()), 0.25));
        }

        state.setLastEmotion(value(state.getHurt()) >= 0.45 ? "hurt_but_listening" : "softened");
        state.setLastSignificantEvent("user_apologized");
    }

    private void updateLastEmotion(AgentSelfState state) {
        if (value(state.getHurt()) >= 0.55) {
            state.setLastEmotion("hurt");
        } else if (value(state.getAnger()) >= 0.35) {
            state.setLastEmotion("upset");
        } else if (value(state.getInsecurity()) >= 0.55) {
            state.setLastEmotion("anxious");
        } else {
            state.setLastEmotion("calm");
        }
    }

    private void normalize(AgentSelfState state) {
        state.setAffection(clamp(state.getAffection()));
        state.setTrust(clamp(state.getTrust()));
        state.setHurt(clamp(state.getHurt()));
        state.setAnger(clamp(state.getAnger()));
        state.setInsecurity(clamp(state.getInsecurity()));
        state.setDisappointment(clamp(state.getDisappointment()));
        state.setEmotionalDistance(clamp(state.getEmotionalDistance()));
    }

    private double value(Double value) {
        return value == null ? 0.0 : value;
    }

    private double clamp(Double value) {
        return Math.max(MIN_VALUE, Math.min(MAX_VALUE, value(value)));
    }

    private int traitValue(Integer value) {
        return value == null ? 5 : value;
    }

    private EmotionTraitModifier emotionTraitModifierOrDefault() {
        return emotionTraitModifier == null ? new EmotionTraitModifier() : emotionTraitModifier;
    }

    private record SelfStateSnapshot(
            Double hurt,
            Double trust,
            Double anger,
            Double insecurity
    ) {

        private static SelfStateSnapshot from(AgentSelfState state) {
            return new SelfStateSnapshot(
                    state.getHurt(),
                    state.getTrust(),
                    state.getAnger(),
                    state.getInsecurity()
            );
        }
    }

    public record EmotionUpdateResult(
            AgentSelfStateSnapshot previousState,
            AgentSelfStateSnapshot nextState,
            EventAnalysis eventAnalysis
    ) {
    }
}

