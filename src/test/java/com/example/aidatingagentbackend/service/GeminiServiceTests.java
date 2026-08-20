package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.config.GeminiProperties;
import com.example.aidatingagentbackend.entity.MemoryChannel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.aidatingagentbackend.exception.GeminiCallException;

class GeminiServiceTests {

    private final GeminiService service = new GeminiService(
            RestClient.create(),
            new GeminiProperties("key", "http://localhost", "model"),
            new ObjectMapper()
    );

    @Test
    void callUsesMinimalThinkingAndLimitsOutputForLowLatency() {
        Map<String, Object> body = service.buildRequestBody("hello", null, MemoryChannel.CALL);

        assertThat(body).containsEntry(
                "generationConfig",
                Map.of(
                        "thinkingConfig", Map.of("thinkingLevel", "minimal"),
                        "maxOutputTokens", 96
                )
        );
    }

    @Test
    void callTopicDisablesThinkingAndLimitsOutputTokens() {
        Map<String, Object> body = service.buildRequestBody("hello", null, MemoryChannel.CALL, 40);

        assertThat(body).containsEntry(
                "generationConfig",
                Map.of(
                        "thinkingConfig", Map.of("thinkingLevel", "minimal"),
                        "maxOutputTokens", 40
                )
        );
    }

    @Test
    void chatKeepsExistingGenerationConfigBehavior() {
        Map<String, Object> body = service.buildRequestBody("hello", null, MemoryChannel.CHAT);

        assertThat(body).doesNotContainKey("generationConfig");
    }

    @Test
    void channelLessInternalCallsKeepExistingGenerationConfigBehavior() {
        Map<String, Object> body = service.buildRequestBody("hello", null, null);

        assertThat(body).doesNotContainKey("generationConfig");
    }

    @Test
    void streamRejectsUpstreamHttpError() {
        TestClient testClient = testClient();
        testClient.server().expect(once(), requestTo(
                        "http://localhost/models/model:streamGenerateContent?alt=sse&key=key"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError().body("upstream failed"));

        assertThatThrownBy(() -> testClient.service().generateStream("hello", chunk -> { }))
                .isInstanceOf(GeminiCallException.class)
                .hasMessageContaining("HTTP 500");
        testClient.server().verify();
    }

    @Test
    void streamRejectsSuccessfulButEmptyResponse() {
        TestClient testClient = testClient();
        testClient.server().expect(once(), requestTo(
                        "http://localhost/models/model:streamGenerateContent?alt=sse&key=key"))
                .andRespond(withSuccess("", MediaType.TEXT_EVENT_STREAM));

        assertThatThrownBy(() -> testClient.service().generateStream("hello", chunk -> { }))
                .isInstanceOf(GeminiCallException.class)
                .hasMessage("Gemini returned an empty response.");
        testClient.server().verify();
    }

    @Test
    void streamForwardsValidChunks() {
        TestClient testClient = testClient();
        String body = "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"안녕\"}]}}]}\n\n";
        testClient.server().expect(once(), requestTo(
                        "http://localhost/models/model:streamGenerateContent?alt=sse&key=key"))
                .andRespond(withSuccess(body, MediaType.TEXT_EVENT_STREAM));
        java.util.List<String> chunks = new java.util.ArrayList<>();

        testClient.service().generateStream("hello", chunks::add);

        assertThat(chunks).containsExactly("안녕");
        testClient.server().verify();
    }

    private TestClient testClient() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiService testService = new GeminiService(
                builder.baseUrl("http://localhost").build(),
                new GeminiProperties("key", "http://localhost", "model"),
                new ObjectMapper());
        return new TestClient(testService, server);
    }

    private record TestClient(GeminiService service, MockRestServiceServer server) {
    }
}
