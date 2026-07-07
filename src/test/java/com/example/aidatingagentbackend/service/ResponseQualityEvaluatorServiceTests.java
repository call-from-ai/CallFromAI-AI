package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.Context;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.ResponseQualityEvaluation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseQualityEvaluatorServiceTests {

    private final ResponseQualityEvaluatorService service =
            new ResponseQualityEvaluatorService(null, null, null);

    @Test
    void fallbackScoresInstantRecoveryLowWhenHurtIsHigh() {
        Context context = contextWithHurt(0.7);

        ResponseQualityEvaluation evaluation = service.fallbackEvaluation(
                1L,
                "아니야 농담이야",
                "괜찮아, 다행이야. 말해줘서 고마워.",
                context,
                false
        );

        assertThat(evaluation.getScore()).isLessThan(0.75);
        assertThat(evaluation.getTooSubmissive()).isTrue();
        assertThat(evaluation.getMatchesSelfState()).isFalse();
    }

    @Test
    void fallbackAllowsBoundedReplyWhenHurtIsHigh() {
        Context context = contextWithHurt(0.7);

        ResponseQualityEvaluation evaluation = service.fallbackEvaluation(
                1L,
                "아니야 농담이야",
                "그런 말은 쉽게 하지 않았으면 좋겠어. 나한텐 꽤 상처였어.",
                context,
                false
        );

        assertThat(evaluation.getScore()).isGreaterThanOrEqualTo(0.75);
        assertThat(evaluation.getBoundaryRespected()).isTrue();
        assertThat(evaluation.getMatchesSelfState()).isTrue();
    }

    private Context contextWithHurt(double hurt) {
        AgentSelfState selfState = new AgentSelfState();
        selfState.setHurt(hurt);
        selfState.setAnger(0.25);
        selfState.setTrust(0.4);
        selfState.setInsecurity(0.6);
        selfState.setLastEmotion("hurt");

        return new Context(
                null,
                null,
                null,
                selfState,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
