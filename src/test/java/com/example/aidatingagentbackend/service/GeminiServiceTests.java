package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.config.GeminiProperties;
import com.example.aidatingagentbackend.entity.MemoryChannel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiServiceTests {

    private final GeminiService service = new GeminiService(
            RestClient.create(),
            new GeminiProperties("key", "http://localhost", "model"),
            new ObjectMapper()
    );

    @Test
    void callDisablesThinking() {
        Map<String, Object> body = service.buildRequestBody("hello", null, MemoryChannel.CALL);

        assertThat(body).containsEntry(
                "generationConfig",
                Map.of("thinkingConfig", Map.of("thinkingBudget", 0))
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
}
