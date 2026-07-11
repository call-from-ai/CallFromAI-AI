package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.engine.EventAnalyzer;
import com.example.aidatingagentbackend.engine.EventDetector;
import com.example.aidatingagentbackend.engine.MessageSignalDetector;
import com.example.aidatingagentbackend.engine.MessageSignalType;
import com.example.aidatingagentbackend.engine.MessageSignals;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.AgentSelfStateLog;
import com.example.aidatingagentbackend.repository.AgentSelfStateLogRepository;
import com.example.aidatingagentbackend.repository.AgentSelfStateRepository;
import com.example.aidatingagentbackend.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class EmotionUpdateService {

    private static final double MIN_VALUE = 0.0;
    private static final double MAX_VALUE = 1.0;
    private static final String BREAKUP_EVENT = "user_declared_breakup";

    private final AgentSelfStateRepository agentSelfStateRepository;
    private final EventAnalyzer eventAnalyzer;
    private final EventDetector eventDetector;
    private final ChatMessageRepository chatMessageRepository;
    private final AgentSelfStateLogRepository agentSelfStateLogRepository;
    private final ReflectionCandidateService reflectionCandidateService;
    private final MessageSignalDetector messageSignalDetector;

    public EmotionUpdateService(
            AgentSelfStateRepository agentSelfStateRepository,
            EventAnalyzer eventAnalyzer,
            EventDetector eventDetector,
            ChatMessageRepository chatMessageRepository,
            AgentSelfStateLogRepository agentSelfStateLogRepository,
            ReflectionCandidateService reflectionCandidateService,
            MessageSignalDetector messageSignalDetector
    ) {
        this.agentSelfStateRepository = agentSelfStateRepository;
        this.eventAnalyzer = eventAnalyzer;
        this.eventDetector = eventDetector;
        this.chatMessageRepository = chatMessageRepository;
        this.agentSelfStateLogRepository = agentSelfStateLogRepository;
        this.reflectionCandidateService = reflectionCandidateService;
        this.messageSignalDetector = messageSignalDetector;
    }

    @Transactional
    public EmotionUpdateResult updateBeforeResponse(Long characterId, String userMessage) {
        AgentSelfState state = agentSelfStateRepository.findByCharacterId(characterId)
                .orElseGet(() -> createDefaultState(characterId));

        SelfStateSnapshot previousSnapshot = SelfStateSnapshot.from(state);
        LocalDateTime now = LocalDateTime.now();
        applyDecay(state, now);
        EventAnalysis eventAnalysis = analyzeEvent(characterId, userMessage, state);
        applyEvent(state, eventAnalysis.eventType());
        applyConversationTransition(state, messageSignalDetector.detect(userMessage), eventAnalysis);
        normalize(state);
        AgentSelfStateLog stateLog = saveLog(characterId, userMessage, eventAnalysis, previousSnapshot, state);
        createReflectionCandidateIfNeeded(characterId, userMessage, eventAnalysis, stateLog);

        return new EmotionUpdateResult(agentSelfStateRepository.save(state), eventAnalysis);
    }

    @Transactional(readOnly = true)
    public AgentSelfState findOrCreatePreview(Long characterId) {
        return agentSelfStateRepository.findByCharacterId(characterId)
                .orElseGet(() -> createDefaultState(characterId));
    }

    private EventAnalysis analyzeEvent(Long characterId, String userMessage, AgentSelfState state) {
        if (eventAnalyzer == null || chatMessageRepository == null) {
            return EventAnalysis.fallback(eventDetector.detect(userMessage));
        }

        return eventAnalyzer.analyze(
                userMessage,
                chatMessageRepository.findTop20ByCharacterIdOrderByCreatedAtDesc(characterId),
                state
        );
    }

    private AgentSelfStateLog saveLog(
            Long userId,
            String userMessage,
            EventAnalysis eventAnalysis,
            SelfStateSnapshot previousSnapshot,
            AgentSelfState nextState
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
        log.setUserMessage(userMessage);
        log.setDeltaReason(buildDeltaReason(eventAnalysis));
        return agentSelfStateLogRepository.save(log);
    }

    private void createReflectionCandidateIfNeeded(
            Long userId,
            String userMessage,
            EventAnalysis eventAnalysis,
            AgentSelfStateLog stateLog
    ) {
        if (reflectionCandidateService == null) {
            return;
        }

        reflectionCandidateService.createIfImportant(userId, userMessage, eventAnalysis, stateLog);
    }

    private String buildDeltaReason(EventAnalysis eventAnalysis) {
        StringBuilder reason = new StringBuilder();
        reason.append("eventType=").append(eventAnalysis.eventType());
        reason.append(", severity=").append(eventAnalysis.severity());
        reason.append(", sincerity=").append(eventAnalysis.sincerity());
        reason.append(", isJoke=").append(eventAnalysis.isJoke());
        reason.append(", isManipulative=").append(eventAnalysis.isManipulative());
        reason.append(", primaryEmotion=").append(eventAnalysis.primaryEmotion());
        reason.append(", summary=").append(eventAnalysis.summary());
        return reason.toString();
    }

    public void applyDecay(AgentSelfState state, LocalDateTime now) {
        if (state.getUpdatedAt() == null || now == null || !now.isAfter(state.getUpdatedAt())) {
            return;
        }

        long hours = Duration.between(state.getUpdatedAt(), now).toHours();
        if (hours <= 0) {
            return;
        }

        double decay = Math.min(0.25, hours * 0.01);
        state.setHurt(value(state.getHurt()) - decay);
        state.setAnger(value(state.getAnger()) - decay);
        state.setInsecurity(value(state.getInsecurity()) - decay * 0.8);
        state.setDisappointment(value(state.getDisappointment()) - decay * 0.7);
        state.setEmotionalDistance(value(state.getEmotionalDistance()) - decay * 0.4);
    }

    public void applyEvent(AgentSelfState state, AgentEventType eventType) {
        switch (eventType) {
            case BREAKUP_DECLARATION -> applyBreakupDeclaration(state);
            case BREAKUP_RETRACTION -> applyBreakupRetraction(state);
            case APOLOGY -> applyApology(state);
            case AFFECTION -> applyAffection(state);
            case INSULT -> applyInsult(state);
            case IGNORE_OR_COLD -> applyColdEvent(state);
            case NORMAL -> updateLastEmotion(state);
        }
    }

    private void applyConversationTransition(
            AgentSelfState state,
            MessageSignals signals,
            EventAnalysis eventAnalysis
    ) {
        if (signals.hasAny(MessageSignalType.AFFECTION, MessageSignalType.USER_RETURNED_TO_TALK)) {
            state.setHurt(value(state.getHurt()) - 0.12);
            state.setAnger(value(state.getAnger()) - 0.08);
            state.setInsecurity(value(state.getInsecurity()) - 0.08);
            state.setTrust(value(state.getTrust()) + 0.06);
            state.setAffection(value(state.getAffection()) + 0.08);
            state.setEmotionalDistance(value(state.getEmotionalDistance()) - 0.08);
            state.setLastEmotion(value(state.getHurt()) > 0.45 ? "guarded_but_softening" : "softened");
            state.setLastSignificantEvent("user_returned_to_talk");
            return;
        }

        if (signals.has(MessageSignalType.APOLOGY)) {
            state.setHurt(value(state.getHurt()) - 0.12);
            state.setAnger(value(state.getAnger()) - 0.1);
            state.setTrust(value(state.getTrust()) + 0.04);
            state.setLastEmotion(value(state.getHurt()) > 0.45 ? "hurt_but_listening" : "softened");
            return;
        }

        if (signals.has(MessageSignalType.ASK_AGENT_SELF_DISCLOSURE)) {
            state.setHurt(value(state.getHurt()) - 0.06);
            state.setAnger(value(state.getAnger()) - 0.04);
            state.setLastEmotion(value(state.getHurt()) > 0.5 ? "guarded_but_talking" : "curious");
            return;
        }

        if (signals.has(MessageSignalType.USER_SKIPPED_MEAL)) {
            state.setInsecurity(value(state.getInsecurity()) + 0.04);
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
            state.setHurt(value(state.getHurt()) - 0.04);
            state.setLastEmotion(value(state.getHurt()) > 0.5 ? "guarded_but_interested" : "interested");
        }
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

    private void applyBreakupDeclaration(AgentSelfState state) {
        state.setHurt(value(state.getHurt()) + 0.7);
        state.setAnger(value(state.getAnger()) + 0.35);
        state.setTrust(value(state.getTrust()) - 0.3);
        state.setInsecurity(value(state.getInsecurity()) + 0.6);
        state.setDisappointment(value(state.getDisappointment()) + 0.45);
        state.setEmotionalDistance(value(state.getEmotionalDistance()) + 0.4);
        state.setLastEmotion("hurt");
        state.setLastSignificantEvent(BREAKUP_EVENT);
    }

    private void applyBreakupRetraction(AgentSelfState state) {
        boolean followsBreakup = BREAKUP_EVENT.equals(state.getLastSignificantEvent());
        state.setHurt(value(state.getHurt()) - 0.1);
        state.setInsecurity(value(state.getInsecurity()) - 0.1);
        state.setAnger(value(state.getAnger()) - 0.05);

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

    private void applyApology(AgentSelfState state) {
        boolean deeplyHurt = value(state.getHurt()) >= 0.5;
        state.setHurt(value(state.getHurt()) - 0.2);
        state.setAnger(value(state.getAnger()) - 0.15);
        state.setTrust(value(state.getTrust()) + 0.05);
        state.setInsecurity(value(state.getInsecurity()) - 0.08);
        state.setDisappointment(value(state.getDisappointment()) - 0.1);

        if (deeplyHurt) {
            state.setHurt(Math.max(value(state.getHurt()), 0.35));
            state.setEmotionalDistance(Math.max(value(state.getEmotionalDistance()), 0.25));
        }

        state.setLastEmotion(value(state.getHurt()) >= 0.45 ? "hurt_but_listening" : "softened");
        state.setLastSignificantEvent("user_apologized");
    }

    private void applyAffection(AgentSelfState state) {
        state.setAffection(value(state.getAffection()) + 0.08);
        state.setTrust(value(state.getTrust()) + 0.03);
        state.setInsecurity(value(state.getInsecurity()) - 0.04);
        state.setEmotionalDistance(value(state.getEmotionalDistance()) - 0.04);
        state.setLastEmotion(value(state.getHurt()) >= 0.5 ? "conflicted" : "affectionate");
    }

    private void applyInsult(AgentSelfState state) {
        state.setHurt(value(state.getHurt()) + 0.35);
        state.setAnger(value(state.getAnger()) + 0.3);
        state.setTrust(value(state.getTrust()) - 0.15);
        state.setDisappointment(value(state.getDisappointment()) + 0.25);
        state.setEmotionalDistance(value(state.getEmotionalDistance()) + 0.2);
        state.setLastEmotion("upset");
        state.setLastSignificantEvent("user_insulted_agent");
    }

    private void applyColdEvent(AgentSelfState state) {
        state.setInsecurity(value(state.getInsecurity()) + 0.2);
        state.setDisappointment(value(state.getDisappointment()) + 0.15);
        state.setEmotionalDistance(value(state.getEmotionalDistance()) + 0.15);
        state.setLastEmotion("distant");
        state.setLastSignificantEvent("user_was_cold");
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
            AgentSelfState agentSelfState,
            EventAnalysis eventAnalysis
    ) {
    }
}
