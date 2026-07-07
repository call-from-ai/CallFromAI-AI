package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventDetector;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EmotionUpdateServiceTests {

    private final EmotionUpdateService emotionUpdateService = new EmotionUpdateService(null, null, new EventDetector(), null, null, null);

    @Test
    void breakupDeclarationRaisesHurtAndInsecurityAndLowersTrust() {
        AgentSelfState state = defaultState();

        emotionUpdateService.applyEvent(state, AgentEventType.BREAKUP_DECLARATION);

        assertThat(state.getHurt()).isGreaterThanOrEqualTo(0.7);
        assertThat(state.getInsecurity()).isGreaterThanOrEqualTo(0.7);
        assertThat(state.getTrust()).isLessThan(0.6);
        assertThat(state.getLastEmotion()).isEqualTo("hurt");
    }

    @Test
    void breakupRetractionDoesNotImmediatelyClearHurt() {
        AgentSelfState state = defaultState();
        emotionUpdateService.applyEvent(state, AgentEventType.BREAKUP_DECLARATION);

        emotionUpdateService.applyEvent(state, AgentEventType.BREAKUP_RETRACTION);

        assertThat(state.getHurt()).isGreaterThanOrEqualTo(0.55);
        assertThat(state.getAnger()).isGreaterThanOrEqualTo(0.25);
        assertThat(state.getLastEmotion()).isEqualTo("hurt");
    }

    @Test
    void apologyPartiallySoftensButDoesNotFullyRecoverDeepHurt() {
        AgentSelfState state = defaultState();
        emotionUpdateService.applyEvent(state, AgentEventType.BREAKUP_DECLARATION);
        emotionUpdateService.applyEvent(state, AgentEventType.BREAKUP_RETRACTION);

        emotionUpdateService.applyEvent(state, AgentEventType.APOLOGY);

        assertThat(state.getHurt()).isGreaterThanOrEqualTo(0.35);
        assertThat(state.getAnger()).isGreaterThan(0.0);
        assertThat(state.getLastEmotion()).isIn("hurt_but_listening", "softened");
    }

    private AgentSelfState defaultState() {
        AgentSelfState state = new AgentSelfState();
        state.setCharacterId(1L);
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
}
