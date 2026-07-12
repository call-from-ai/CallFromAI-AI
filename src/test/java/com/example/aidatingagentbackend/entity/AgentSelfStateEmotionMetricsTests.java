package com.example.aidatingagentbackend.entity;

import com.example.aidatingagentbackend.engine.MemoryEngine;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AgentSelfStateEmotionMetricsTests {

    private final MemoryEngine memoryEngine = new MemoryEngine();

    @ParameterizedTest
    @MethodSource("scenarios")
    void calculatesApprovedIntensityAndPreservesMemoryCreationThreshold(
            String emotion, double affection, double trust, double hurt, double anger,
            double insecurity, double disappointment, double distance,
            String conversation, int expectedIntensity, boolean expectedMemoryCreation
    ) {
        AgentSelfState state = state(emotion, affection, trust, hurt, anger, insecurity, disappointment, distance);

        assertThat(state.representativeEmotion()).isEqualTo(emotion);
        assertThat(state.emotionIntensity()).isEqualTo(expectedIntensity);
        assertThat(memoryEngine.analyze(conversation, state.representativeEmotion(), state.emotionIntensity()).shouldCreate())
                .isEqualTo(expectedMemoryCreation);
    }

    private static Stream<Arguments> scenarios() {
        return Stream.of(
                Arguments.of("calm", 0.55, 0.60, 0.0, 0.0, 0.15, 0.0, 0.15, "오늘 뭐해", 0, false),
                Arguments.of("upset", 0.55, 0.45, 0.35, 0.30, 0.15, 0.25, 0.35, "너 진짜 바보야", 3, false),
                Arguments.of("hurt", 0.55, 0.30, 0.70, 0.35, 0.75, 0.45, 0.55, "헤어지자", 6, true),
                Arguments.of("distant", 0.55, 0.60, 0.0, 0.0, 0.35, 0.15, 0.30, "왜 답장이 늦어", 2, false),
                Arguments.of("affectionate", 0.63, 0.63, 0.0, 0.0, 0.11, 0.0, 0.11, "좋아해", 1, false),
                Arguments.of("softened", 0.55, 0.65, 0.0, 0.0, 0.07, 0.0, 0.15, "미안해", 1, false)
        );
    }

    private static AgentSelfState state(String emotion, double affection, double trust, double hurt, double anger,
                                        double insecurity, double disappointment, double distance) {
        AgentSelfState state = new AgentSelfState();
        state.setLastEmotion(emotion);
        state.setAffection(affection);
        state.setTrust(trust);
        state.setHurt(hurt);
        state.setAnger(anger);
        state.setInsecurity(insecurity);
        state.setDisappointment(disappointment);
        state.setEmotionalDistance(distance);
        return state;
    }
}
