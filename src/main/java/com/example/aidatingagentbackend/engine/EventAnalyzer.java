package com.example.aidatingagentbackend.engine;

import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.ChatMessage;
import com.example.aidatingagentbackend.service.GeminiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventAnalyzer {

    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.8;

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;
    private final EventDetector fallbackDetector;

    public EventAnalyzer(
            GeminiService geminiService,
            ObjectMapper objectMapper,
            EventDetector fallbackDetector
    ) {
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
        this.fallbackDetector = fallbackDetector;
    }

    public EventAnalysis analyze(
            String userMessage,
            List<ChatMessage> recentHistory,
            AgentSelfState currentSelfState
    ) {
        EventDetector.RuleBasedEventDetection ruleDetection = fallbackDetector.detectWithConfidence(userMessage);
        if (canSkipLlm(ruleDetection, currentSelfState)) {
            return EventAnalysis.fallback(ruleDetection.eventType());
        }

        try {
            String response = geminiService.generate(buildPrompt(userMessage, recentHistory, currentSelfState));
            return parse(response);
        } catch (RuntimeException exception) {
            return EventAnalysis.fallback(ruleDetection.eventType());
        }
    }

    private boolean canSkipLlm(
            EventDetector.RuleBasedEventDetection ruleDetection,
            AgentSelfState currentSelfState
    ) {
        if (ruleDetection == null) {
            return false;
        }
        if (ruleDetection.confidence() >= HIGH_CONFIDENCE_THRESHOLD) {
            return true;
        }
        if (Boolean.TRUE.equals(ruleDetection.needsLlmClarification())) {
            return false;
        }

        return !hasSensitiveSelfState(currentSelfState);
    }

    private boolean hasSensitiveSelfState(AgentSelfState currentSelfState) {
        if (currentSelfState == null) {
            return false;
        }

        return value(currentSelfState.getHurt()) > 0.5
                || value(currentSelfState.getAnger()) > 0.4
                || value(currentSelfState.getInsecurity()) > 0.6;
    }

    private String buildPrompt(
            String userMessage,
            List<ChatMessage> recentHistory,
            AgentSelfState currentSelfState
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an event analyzer for a romantic AI agent backend.\n");
        prompt.append("Analyze the latest user message in context of recent chat history and current agent self state.\n");
        prompt.append("Return JSON only. No markdown, no extra text.\n\n");
        prompt.append("Allowed eventType values:\n");
        prompt.append("- BREAKUP_DECLARATION\n");
        prompt.append("- BREAKUP_RETRACTION\n");
        prompt.append("- APOLOGY\n");
        prompt.append("- AFFECTION\n");
        prompt.append("- INSULT\n");
        prompt.append("- IGNORE_OR_COLD\n");
        prompt.append("- NORMAL\n\n");
        prompt.append("Field rules:\n");
        prompt.append("- severity: number from 0.0 to 1.0 showing emotional impact on the agent.\n");
        prompt.append("- sincerity: number from 0.0 to 1.0 estimating whether the user means it sincerely.\n");
        prompt.append("- isJoke: true only when the message is likely a joke or playful retraction.\n");
        prompt.append("- isManipulative: true when the message uses emotional pressure, threats, or push-pull control.\n");
        prompt.append("- primaryEmotion: likely immediate agent emotion, such as hurt, upset, relieved, affection, distant, neutral.\n");
        prompt.append("- summary: short Korean summary of the detected event.\n\n");
        appendSelfState(prompt, currentSelfState);
        appendRecentHistory(prompt, recentHistory);
        prompt.append("[Latest User Message]\n");
        prompt.append(userMessage == null ? "" : userMessage).append("\n\n");
        prompt.append("JSON schema:\n");
        prompt.append("{\"eventType\":\"BREAKUP_DECLARATION\",\"severity\":0.9,\"sincerity\":0.8,");
        prompt.append("\"isJoke\":false,\"isManipulative\":false,\"primaryEmotion\":\"hurt\",");
        prompt.append("\"summary\":\"사용자가 이별을 선언했다.\"}\n");
        return prompt.toString();
    }

    private void appendSelfState(StringBuilder prompt, AgentSelfState state) {
        prompt.append("[Current Agent Self State]\n");
        if (state == null) {
            prompt.append("none\n\n");
            return;
        }

        appendLine(prompt, "affection", state.getAffection());
        appendLine(prompt, "trust", state.getTrust());
        appendLine(prompt, "hurt", state.getHurt());
        appendLine(prompt, "anger", state.getAnger());
        appendLine(prompt, "insecurity", state.getInsecurity());
        appendLine(prompt, "disappointment", state.getDisappointment());
        appendLine(prompt, "emotionalDistance", state.getEmotionalDistance());
        appendLine(prompt, "lastEmotion", state.getLastEmotion());
        appendLine(prompt, "lastSignificantEvent", state.getLastSignificantEvent());
        prompt.append("\n");
    }

    private void appendRecentHistory(StringBuilder prompt, List<ChatMessage> recentHistory) {
        prompt.append("[Recent History]\n");
        if (recentHistory == null || recentHistory.isEmpty()) {
            prompt.append("none\n\n");
            return;
        }

        recentHistory.stream()
                .limit(10)
                .forEach(message -> prompt.append(message.getRole())
                        .append(": ")
                        .append(message.getContent())
                        .append("\n"));
        prompt.append("\n");
    }

    private void appendLine(StringBuilder prompt, String label, Object value) {
        if (value != null) {
            prompt.append(label).append(": ").append(value).append("\n");
        }
    }

    private EventAnalysis parse(String response) {
        JsonNode json = readJson(response);
        AgentEventType eventType = parseEventType(json.path("eventType").asText(null));
        return new EventAnalysis(
                eventType,
                clamp(json.path("severity").asDouble(EventAnalysis.fallback(eventType).severity())),
                clamp(json.path("sincerity").asDouble(EventAnalysis.fallback(eventType).sincerity())),
                json.path("isJoke").asBoolean(false),
                json.path("isManipulative").asBoolean(false),
                json.path("primaryEmotion").asText(EventAnalysis.fallback(eventType).primaryEmotion()),
                json.path("summary").asText(EventAnalysis.fallback(eventType).summary())
        );
    }

    private JsonNode readJson(String response) {
        try {
            return objectMapper.readTree(extractJson(response));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse event analysis response.", exception);
        }
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

    private AgentEventType parseEventType(String value) {
        if (value == null || value.isBlank()) {
            return AgentEventType.NORMAL;
        }

        try {
            return AgentEventType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return AgentEventType.NORMAL;
        }
    }

    private EventAnalysis fallback(String userMessage) {
        return EventAnalysis.fallback(fallbackDetector.detect(userMessage));
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double value(Double value) {
        return value == null ? 0.0 : value;
    }
}
