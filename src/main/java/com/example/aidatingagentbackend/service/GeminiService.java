package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.config.GeminiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class GeminiService {

    private final RestClient restClient;
    private final GeminiProperties properties;

    public GeminiService(
            RestClient restClient,
            GeminiProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public String generate(String prompt) {

        if (!StringUtils.hasText(properties.apiKey())) {
            throw new IllegalStateException("Gemini API Key가 없습니다.");
        }

        JsonNode response =
                restClient.post()
                        .uri(uriBuilder ->
                                uriBuilder.path("/models/{model}:generateContent")
                                        .queryParam("key", properties.apiKey())
                                        .build(properties.model()))
                        .body(Map.of(
                                "contents",
                                new Object[]{
                                        Map.of(
                                                "parts",
                                                new Object[]{
                                                        Map.of(
                                                                "text",
                                                                prompt
                                                        )
                                                }
                                        )
                                }
                        ))
                        .retrieve()
                        .body(JsonNode.class);

        return response
                .path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText();

    }

}