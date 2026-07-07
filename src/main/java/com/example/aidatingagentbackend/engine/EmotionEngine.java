package com.example.aidatingagentbackend.engine;

import com.example.aidatingagentbackend.entity.State;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmotionEngine {

    private static final int MIN_INTENSITY = 0;
    private static final int MAX_INTENSITY = 10;
    private static final String DEFAULT_EMOTION = "neutral";

    private static final List<EmotionRule> RULES = List.of(
            new EmotionRule("sadness", 5, List.of("헤어지자", "그만 만나", "끝내자", "이별")),
            new EmotionRule("hurt", 5, List.of("배신", "거짓말", "속였", "믿었는데")),
            new EmotionRule("relieved", -2, List.of("사과", "미안", "죄송", "잘못했")),
            new EmotionRule("happy", 2, List.of("칭찬", "멋져", "예뻐", "고마워", "좋아해")),
            new EmotionRule("jealousy", 3, List.of("질투", "다른 사람", "전 애인", "전남친", "전여친")),
            new EmotionRule("anxiety", 3, List.of("답장 늦음", "답장이 늦", "왜 늦게", "읽씹", "안읽씹"))
    );

    public EmotionResult analyze(State currentState, String userMessage) {
        String currentEmotion = resolveCurrentEmotion(currentState);
        int currentIntensity = resolveCurrentIntensity(currentState);

        if (userMessage == null || userMessage.isBlank()) {
            return new EmotionResult(currentEmotion, currentIntensity);
        }

        EmotionResult ruleResult = analyzeWithRules(userMessage, currentEmotion, currentIntensity);
        if (!ruleResult.emotion().equals(currentEmotion)
                || !ruleResult.emotionIntensity().equals(currentIntensity)) {
            return ruleResult;
        }

        return ruleResult;
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
