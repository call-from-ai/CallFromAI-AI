package com.example.aidatingagentbackend.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventDetectorTests {

    private final EventDetector eventDetector = new EventDetector();

    @Test
    void detectsBreakupDeclaration() {
        assertThat(eventDetector.detect("우리 이제 헤어지자"))
                .isEqualTo(AgentEventType.BREAKUP_DECLARATION);
    }

    @Test
    void detectsBreakupRetraction() {
        assertThat(eventDetector.detect("아니야 농담이야. 방금 말 취소할게"))
                .isEqualTo(AgentEventType.BREAKUP_RETRACTION);
    }

    @Test
    void detectsApology() {
        assertThat(eventDetector.detect("미안해 내가 잘못했어"))
                .isEqualTo(AgentEventType.APOLOGY);
    }
}
