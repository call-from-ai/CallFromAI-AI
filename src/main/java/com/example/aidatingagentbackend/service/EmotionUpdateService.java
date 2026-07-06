package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventDetector;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.repository.AgentSelfStateRepository;
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
    private final EventDetector eventDetector;

    public EmotionUpdateService(
            AgentSelfStateRepository agentSelfStateRepository,
            EventDetector eventDetector
    ) {
        this.agentSelfStateRepository = agentSelfStateRepository;
        this.eventDetector = eventDetector;
    }

    @Transactional
    public AgentSelfState updateBeforeResponse(Long characterId, String userMessage) {
        AgentSelfState state = agentSelfStateRepository.findByCharacterId(characterId)
                .orElseGet(() -> createDefaultState(characterId));

        LocalDateTime now = LocalDateTime.now();
        applyDecay(state, now);
        AgentEventType eventType = eventDetector.detect(userMessage);
        applyEvent(state, eventType);
        normalize(state);

        return agentSelfStateRepository.save(state);
    }

    @Transactional(readOnly = true)
    public AgentSelfState findOrCreatePreview(Long characterId) {
        return agentSelfStateRepository.findByCharacterId(characterId)
                .orElseGet(() -> createDefaultState(characterId));
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
}
