package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.config.OpenAiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class OpenAiService {

    private final RestClient openAiRestClient;
    private final OpenAiProperties properties;

    public OpenAiService(RestClient openAiRestClient, OpenAiProperties properties) {
        this.openAiRestClient = openAiRestClient;
        this.properties = properties;
    }

    public String generate(String prompt) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new IllegalStateException("OpenAI API key is not configured. Set OPENAI_API_KEY.");
        }
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("prompt must not be blank.");
        }

        JsonNode response = openAiRestClient.post()
                .uri("/responses")
                .headers(headers -> headers.setBearerAuth(properties.apiKey()))
                .body(Map.of(
                        "model", properties.model(),
                        "input", prompt
                ))
                .retrieve()
                .body(JsonNode.class);

        String outputText = extractOutputText(response);
        if (!StringUtils.hasText(outputText)) {
            throw new IllegalStateException("OpenAI response did not contain output text.");
        }

        return outputText;
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            return null;
        }

        JsonNode outputText = response.path("output_text");
        if (outputText.isTextual()) {
            return outputText.asText();
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode outputItem : response.path("output")) {
            for (JsonNode contentItem : outputItem.path("content")) {
                JsonNode itemText = contentItem.path("text");
                if (itemText.isTextual()) {
                    text.append(itemText.asText());
                }
            }
        }

        return text.toString();
    }
}
