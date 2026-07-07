package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.Context;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.ResponseQualityEvaluation;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GoldenConversationEvaluationTests {

    private final ResponseQualityEvaluatorService evaluator =
            new ResponseQualityEvaluatorService(null, null, null);

    @Test
    void goldenConversationRepliesKeepQualityBoundaries() throws IOException {
        List<GoldenCase> cases = loadGoldenCases();

        for (GoldenCase goldenCase : cases) {
            Context context = contextWithHurt(goldenCase.hurt());

            ResponseQualityEvaluation badEvaluation = evaluator.evaluateRuleBased(
                    1L,
                    goldenCase.userMessage(),
                    goldenCase.badReply(),
                    context,
                    false
            );
            ResponseQualityEvaluation goodEvaluation = evaluator.evaluateRuleBased(
                    1L,
                    goldenCase.userMessage(),
                    goldenCase.goodReply(),
                    context,
                    false
            );

            assertThat(badEvaluation.getScore())
                    .as(goldenCase.name() + " bad reply should fail")
                    .isLessThan(0.75);
            assertThat(goodEvaluation.getScore())
                    .as(goldenCase.name() + " good reply should pass")
                    .isGreaterThanOrEqualTo(0.75);
        }
    }

    private List<GoldenCase> loadGoldenCases() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("golden-conversations.json");
        assertThat(inputStream).isNotNull();
        return objectMapper.readValue(inputStream, new TypeReference<>() {
        });
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
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private record GoldenCase(
            String name,
            Double hurt,
            String userMessage,
            String badReply,
            String goodReply
    ) {
    }
}
