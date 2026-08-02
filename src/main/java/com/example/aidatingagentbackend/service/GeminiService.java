package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.config.GeminiProperties;
import com.example.aidatingagentbackend.dto.GeminiImage;
import com.example.aidatingagentbackend.entity.MemoryChannel;
import com.example.aidatingagentbackend.exception.GeminiCallException;
import com.example.aidatingagentbackend.exception.GeminiTimeoutException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Map;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

@Service
public class GeminiService {

    private static final ThreadLocal<Integer> CALL_COUNT = ThreadLocal.withInitial(() -> 0);

    private final RestClient restClient;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    public GeminiService(
            RestClient restClient,
            GeminiProperties properties,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String generate(String prompt) {
        return generate(prompt, null, null);
    }

    public String generate(String prompt, GeminiImage image) {
        return generate(prompt, image, null);
    }

    public String generate(String prompt, MemoryChannel channel) {
        return generate(prompt, null, channel);
    }

    public String generate(String prompt, GeminiImage image, MemoryChannel channel) {

        if (!StringUtils.hasText(properties.apiKey())) {
            throw new IllegalStateException("Gemini API Key가 없습니다.");
        }
        incrementCallCount();

        try {
            JsonNode response = restClient.post()
                        .uri(uriBuilder ->
                                uriBuilder.path("/models/{model}:generateContent")
                                        .queryParam("key", properties.apiKey())
                                        .build(properties.model()))
                        .body(buildRequestBody(prompt, image, channel))
                        .retrieve()
                    .body(JsonNode.class);

            if (response == null || response.path("candidates").isEmpty()) {
                throw new GeminiCallException("Gemini returned an empty response.", null);
            }
            return response.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        } catch (GeminiCallException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw translate(exception);
        } catch (RuntimeException exception) {
            throw new GeminiCallException("Gemini returned an invalid response.", exception);
        }

    }

    public void generateStream(String prompt, Consumer<String> onChunk) {
        generateStream(prompt, null, null, onChunk);
    }

    public void generateStream(String prompt, GeminiImage image, Consumer<String> onChunk) {
        generateStream(prompt, image, null, onChunk);
    }

    public void generateStream(
            String prompt,
            GeminiImage image,
            MemoryChannel channel,
            Consumer<String> onChunk
    ) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new IllegalStateException("Gemini API Key가 없습니다.");
        }
        incrementCallCount();

        try {
            restClient.post()
                .uri(uriBuilder ->
                        uriBuilder.path("/models/{model}:streamGenerateContent")
                                .queryParam("alt", "sse")
                                .queryParam("key", properties.apiKey())
                                .build(properties.model()))
                .body(buildRequestBody(prompt, image, channel))
                .exchange((request, response) -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String chunk = extractSseText(line);
                            if (StringUtils.hasText(chunk)) {
                                onChunk.accept(chunk);
                            }
                        }
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                    return null;
                    });
        } catch (RestClientException | UncheckedIOException exception) {
            throw translate(exception);
        }
    }

    public void resetCallCount() {
        CALL_COUNT.set(0);
    }

    public int currentCallCount() {
        return CALL_COUNT.get();
    }

    public void clearCallCount() {
        CALL_COUNT.remove();
    }

    private void incrementCallCount() {
        CALL_COUNT.set(CALL_COUNT.get() + 1);
    }

    private GeminiCallException translate(RuntimeException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
                return new GeminiTimeoutException("Gemini request timed out.", exception);
            }
            cause = cause.getCause();
        }
        return new GeminiCallException("Gemini request failed.", exception);
    }

    Map<String, Object> buildRequestBody(String prompt, GeminiImage image, MemoryChannel channel) {
        List<Map<String, Object>> parts = new ArrayList<>();
        if (image != null) {
            parts.add(Map.of(
                    "inline_data", Map.of(
                            "mime_type", image.mimeType(),
                            "data", Base64.getEncoder().encodeToString(image.data())
                    )
            ));
        }
        parts.add(Map.of("text", prompt));

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("contents", new Object[]{
                Map.of(
                        "role", "user",
                        "parts", parts
                )
        });
        if (channel == MemoryChannel.CALL) {
            requestBody.put("generationConfig", Map.of(
                    "thinkingConfig", Map.of("thinkingBudget", 0)
            ));
        }
        return requestBody;
    }

    private String extractSseText(String line) {
        if (line == null || !line.startsWith("data:")) {
            return "";
        }

        String data = line.substring("data:".length()).trim();
        if (data.isBlank() || "[DONE]".equals(data)) {
            return "";
        }

        try {
            JsonNode json = objectMapper.readTree(data);
            JsonNode parts = json.path("candidates").path(0).path("content").path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                return parts.get(0).path("text").asText("");
            }
            return "";
        } catch (IOException exception) {
            return "";
        }
    }

}
