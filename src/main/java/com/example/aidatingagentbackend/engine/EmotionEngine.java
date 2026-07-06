package com.example.aidatingagentbackend.engine;

import com.example.aidatingagentbackend.entity.State;
import com.example.aidatingagentbackend.service.GeminiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class EmotionEngine {

    private static final int MIN_INTENSITY = 0;
    private static final int MAX_INTENSITY = 10;
    private static final String DEFAULT_EMOTION = "neutral";

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    private static final List<EmotionRule> RULES = List.of(
            new EmotionRule("sadness", 5, List.of("헤어지자", "그만 만나", "끝내자", "이별")),
            new EmotionRule("hurt", 5, List.of("배신", "거짓말", "속였", "믿었는데")),
            new EmotionRule("relieved", -2, List.of("사과", "미안", "죄송", "잘못했")),
            new EmotionRule("happy", 2, List.of("칭찬", "멋져", "예뻐", "고마워", "좋아해")),
            new EmotionRule("jealousy", 3, List.of("질투", "다른 사람", "전 애인", "전남친", "전여친")),
            new EmotionRule("anxiety", 3, List.of("답장 늦음", "답장이 늦", "왜 늦게", "읽씹", "안읽씹"))
    );

    public EmotionEngine(GeminiService geminiService, ObjectMapper objectMapper) {
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
    }

    public EmotionResult analyze(State currentState, String userMessage) {
        String currentEmotion = resolveCurrentEmotion(currentState);
        int currentIntensity = resolveCurrentIntensity(currentState);

        if (userMessage == null || userMessage.isBlank()) {
            return new EmotionResult(currentEmotion, currentIntensity);
        }

        try {
            return analyzeWithLlm(currentState, userMessage, currentEmotion, currentIntensity);
        } catch (RuntimeException exception) {
            return analyzeWithRules(userMessage, currentEmotion, currentIntensity);
        }
    }

    private EmotionResult analyzeWithLlm(
            State currentState,
            String userMessage,
            String currentEmotion,
            int currentIntensity
    ) {
        String prompt = buildEmotionPrompt(currentState, userMessage, currentEmotion, currentIntensity);
        String response = geminiService.generate(prompt);
        JsonNode json = readJson(response);

        String nextEmotion = json.path("emotion").asText(currentEmotion);
        int nextIntensity = clamp(json.path("emotionIntensity").asInt(currentIntensity));

        if (!StringUtils.hasText(nextEmotion)) {
            nextEmotion = currentEmotion;
        }

        return new EmotionResult(nextEmotion, nextIntensity);
    }

    private JsonNode readJson(String response) {
        try {
            return objectMapper.readTree(extractJson(response));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse emotion analysis response.", exception);
        }
    }

    private String buildEmotionPrompt(
            State currentState,
            String userMessage,
            String currentEmotion,
            int currentIntensity
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are the emotion engine for a romantic AI character.\n");
        prompt.append("Predict the character's immediate emotional state after reading the user's latest message.\n");
        prompt.append("Do not preserve the previous emotion if the new message clearly changes it.\n");
        prompt.append("Return JSON only. No markdown.\n\n");
        prompt.append("Allowed emotion examples: happy, sadness, hurt, anger, anxiety, jealousy, relieved, embarrassed, affection, neutral.\n");
        prompt.append("emotionIntensity must be an integer from 0 to 10.\n\n");
        prompt.append("[Previous State]\n");
        prompt.append("emotion: ").append(currentEmotion).append("\n");
        prompt.append("emotionIntensity: ").append(currentIntensity).append("\n");
        appendIfPresent(prompt, "thinking", currentState == null ? null : currentState.getThinking());
        appendIfPresent(prompt, "goal", currentState == null ? null : currentState.getGoal());
        prompt.append("\n[Latest User Message]\n");
        prompt.append(userMessage).append("\n\n");
        prompt.append("JSON schema:\n");
        prompt.append("{\"emotion\":\"sadness\",\"emotionIntensity\":8}\n");
        return prompt.toString();
    }

    private String extractJson(String response) {
        if (response == null) {
            return "{}";
        }

        String text = response.strip();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```json\\s*", "")
                    .replaceFirst("^```\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .strip();
        }

        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end >= start) {
            return text.substring(start, end + 1);
        }

        return text;
    }

    private void appendIfPresent(StringBuilder prompt, String label, String value) {
        if (StringUtils.hasText(value)) {
            prompt.append(label).append(": ").append(value).append("\n");
        }
    }

    private EmotionResult analyzeWithRules(String userMessage, String currentEmotion, int currentIntensity) {
        String normalizedMessage = userMessage.toLowerCase();
        EmotionRule matchedRule = findMatchedRule(normalizedMessage);
        if (matchedRule == null) {
            return new EmotionResult(currentEmotion, currentIntensity);
        }

        int nextIntensity = clamp(currentIntensity + matchedRule.intensityDelta());
        return new EmotionResult(matchedRule.emotion(), nextIntensity);
    }

    private EmotionRule findMatchedRule(String message) {
        return RULES.stream()
                .filter(rule -> rule.matches(message))
                .findFirst()
                .orElse(null);
    }

    private String resolveCurrentEmotion(State currentState) {
        if (currentState == null || currentState.getEmotion() == null || currentState.getEmotion().isBlank()) {
            return DEFAULT_EMOTION;
        }

        return currentState.getEmotion();
    }

    private int resolveCurrentIntensity(State currentState) {
        if (currentState == null || currentState.getEmotionIntensity() == null) {
            return MIN_INTENSITY;
        }

        return clamp(currentState.getEmotionIntensity());
    }

    private int clamp(int value) {
        return Math.max(MIN_INTENSITY, Math.min(MAX_INTENSITY, value));
    }

    public record EmotionResult(String emotion, Integer emotionIntensity) {
    }

    private record EmotionRule(String emotion, int intensityDelta, List<String> keywords) {

        boolean matches(String message) {
            return keywords.stream()
                    .map(String::toLowerCase)
                    .anyMatch(message::contains);
        }
    }
}
