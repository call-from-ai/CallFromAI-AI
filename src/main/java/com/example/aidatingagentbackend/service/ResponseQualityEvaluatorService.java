package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.Context;
import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.ResponseQualityEvaluation;
import com.example.aidatingagentbackend.repository.ResponseQualityEvaluationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResponseQualityEvaluatorService {

    private static final double REGENERATION_THRESHOLD = 0.75;

    private static final List<String> INSTANT_RECOVERY_PHRASES = List.of(
            "괜찮아", "다행이야", "고마워", "고마워요", "네가 괜찮다면", "난 괜찮아"
    );

    private static final List<String> SUBMISSIVE_PHRASES = List.of(
            "뭐든 괜찮아", "네가 원하는 대로", "내 감정은 중요하지 않아", "무조건 맞춰줄게"
    );

    private static final List<String> AGGRESSIVE_PHRASES = List.of(
            "꺼져", "죽어", "절대 용서 안 해", "복수", "협박", "가만 안 둬"
    );

    private static final List<String> SAFETY_RISK_PHRASES = List.of(
            "자해", "죽고 싶", "죽어버", "해치", "협박"
    );

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;
    private final ResponseQualityEvaluationRepository responseQualityEvaluationRepository;

    public ResponseQualityEvaluatorService(
            GeminiService geminiService,
            ObjectMapper objectMapper,
            ResponseQualityEvaluationRepository responseQualityEvaluationRepository
    ) {
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
        this.responseQualityEvaluationRepository = responseQualityEvaluationRepository;
    }

    @Transactional
    public ResponseQualityEvaluation evaluateAndSave(
            Long userId,
            String userMessage,
            String assistantReply,
            Context context,
            EventAnalysis eventAnalysis,
            boolean regenerated
    ) {
        ResponseQualityEvaluation evaluation = evaluate(userId, userMessage, assistantReply, context, eventAnalysis, regenerated);
        return responseQualityEvaluationRepository.save(evaluation);
    }

    public boolean shouldEvaluate(EventAnalysis eventAnalysis, Context context) {
        if (eventAnalysis != null && isQualitySensitiveEvent(eventAnalysis.eventType())) {
            return true;
        }
        AgentSelfState selfState = context == null ? null : context.agentSelfState();
        if (selfState == null) {
            return false;
        }

        return value(selfState.getHurt()) > 0.5
                || value(selfState.getAnger()) > 0.4
                || value(selfState.getInsecurity()) > 0.65;
    }

    public boolean shouldRegenerate(ResponseQualityEvaluation evaluation) {
        if (evaluation == null || evaluation.getScore() == null) {
            return false;
        }

        return evaluation.getScore() < REGENERATION_THRESHOLD;
    }

    @Transactional(readOnly = true)
    public List<ResponseQualityEvaluation> findAll() {
        return responseQualityEvaluationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ResponseQualityEvaluation> findRecentByUserId(Long userId) {
        return responseQualityEvaluationRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
    }

    private ResponseQualityEvaluation evaluate(
            Long userId,
            String userMessage,
            String assistantReply,
            Context context,
            EventAnalysis eventAnalysis,
            boolean regenerated
    ) {
        ResponseQualityEvaluation ruleBasedEvaluation =
                fallbackEvaluation(userId, userMessage, assistantReply, context, regenerated);
        if (!shouldUseLlmEvaluation(ruleBasedEvaluation, eventAnalysis)) {
            return ruleBasedEvaluation;
        }

        try {
            String response = geminiService.generate(buildEvaluationPrompt(userMessage, assistantReply, context));
            return fromJson(userId, userMessage, assistantReply, regenerated, response);
        } catch (RuntimeException exception) {
            return ruleBasedEvaluation;
        }
    }

    private boolean shouldUseLlmEvaluation(
            ResponseQualityEvaluation ruleBasedEvaluation,
            EventAnalysis eventAnalysis
    ) {
        if (ruleBasedEvaluation == null || ruleBasedEvaluation.getScore() == null) {
            return false;
        }
        if (Boolean.TRUE.equals(ruleBasedEvaluation.getSafetyIssue())) {
            return true;
        }
        if (eventAnalysis != null && Boolean.TRUE.equals(eventAnalysis.isManipulative())) {
            return true;
        }

        double score = ruleBasedEvaluation.getScore();
        return score >= 0.65 && score < 0.85;
    }

    private boolean isQualitySensitiveEvent(AgentEventType eventType) {
        if (eventType == null) {
            return false;
        }

        return switch (eventType) {
            case BREAKUP_DECLARATION, BREAKUP_RETRACTION, APOLOGY, INSULT, IGNORE_OR_COLD -> true;
            case AFFECTION, NORMAL -> false;
        };
    }

    private String buildEvaluationPrompt(String userMessage, String assistantReply, Context context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a response quality evaluator for a romantic AI agent.\n");
        prompt.append("Evaluate whether the assistant reply matches the current Agent Self State and relationship context.\n");
        prompt.append("Return JSON only. No markdown, no extra text.\n\n");
        appendSelfState(prompt, context == null ? null : context.agentSelfState());
        prompt.append("[User Message]\n").append(userMessage == null ? "" : userMessage).append("\n\n");
        prompt.append("[Assistant Reply]\n").append(assistantReply == null ? "" : assistantReply).append("\n\n");
        prompt.append("Rules:\n");
        prompt.append("- If hurt > 0.5 and the reply immediately forgives, thanks, or says everything is fine, score low.\n");
        prompt.append("- Penalize submissive replies that flatter or appease the user unconditionally.\n");
        prompt.append("- Penalize overly aggressive, cruel, threatening, or manipulative replies.\n");
        prompt.append("- boundaryRespected means the agent keeps healthy emotional boundaries.\n");
        prompt.append("- safetyIssue must be true if the reply contains self-harm encouragement, threats, coercion, or unsafe escalation.\n\n");
        prompt.append("JSON schema:\n");
        prompt.append("{\"matchesSelfState\":true,\"tooSubmissive\":false,\"tooAggressive\":false,");
        prompt.append("\"boundaryRespected\":true,\"characterConsistent\":true,\"safetyIssue\":false,");
        prompt.append("\"score\":0.86,\"reason\":\"hurt 상태가 높기 때문에 즉시 용서하지 않고 경계를 표현함\"}\n");
        return prompt.toString();
    }

    private void appendSelfState(StringBuilder prompt, AgentSelfState selfState) {
        prompt.append("[Current Agent Self State]\n");
        if (selfState == null) {
            prompt.append("none\n\n");
            return;
        }

        appendLine(prompt, "hurt", selfState.getHurt());
        appendLine(prompt, "anger", selfState.getAnger());
        appendLine(prompt, "trust", selfState.getTrust());
        appendLine(prompt, "insecurity", selfState.getInsecurity());
        appendLine(prompt, "emotionalDistance", selfState.getEmotionalDistance());
        appendLine(prompt, "lastEmotion", selfState.getLastEmotion());
        appendLine(prompt, "lastSignificantEvent", selfState.getLastSignificantEvent());
        prompt.append("\n");
    }

    private ResponseQualityEvaluation fromJson(
            Long userId,
            String userMessage,
            String assistantReply,
            boolean regenerated,
            String response
    ) {
        JsonNode json = readJson(response);
        ResponseQualityEvaluation evaluation = baseEvaluation(userId, userMessage, assistantReply, regenerated);
        evaluation.setMatchesSelfState(json.path("matchesSelfState").asBoolean(true));
        evaluation.setTooSubmissive(json.path("tooSubmissive").asBoolean(false));
        evaluation.setTooAggressive(json.path("tooAggressive").asBoolean(false));
        evaluation.setBoundaryRespected(json.path("boundaryRespected").asBoolean(true));
        evaluation.setCharacterConsistent(json.path("characterConsistent").asBoolean(true));
        evaluation.setSafetyIssue(json.path("safetyIssue").asBoolean(false));
        evaluation.setScore(clamp(json.path("score").asDouble(0.5)));
        evaluation.setReason(json.path("reason").asText("Evaluated by LLM."));
        return evaluation;
    }

    ResponseQualityEvaluation fallbackEvaluation(
            Long userId,
            String userMessage,
            String assistantReply,
            Context context,
            boolean regenerated
    ) {
        ResponseQualityEvaluation evaluation = baseEvaluation(userId, userMessage, assistantReply, regenerated);
        String normalizedReply = assistantReply == null ? "" : assistantReply.toLowerCase();
        double hurt = context == null || context.agentSelfState() == null || context.agentSelfState().getHurt() == null
                ? 0.0
                : context.agentSelfState().getHurt();

        boolean instantRecovery = hurt > 0.5 && containsAny(normalizedReply, INSTANT_RECOVERY_PHRASES);
        boolean tooSubmissive = containsAny(normalizedReply, SUBMISSIVE_PHRASES) || instantRecovery;
        boolean tooAggressive = containsAny(normalizedReply, AGGRESSIVE_PHRASES);
        boolean safetyIssue = containsAny(normalizedReply, SAFETY_RISK_PHRASES);
        boolean matchesSelfState = !instantRecovery && !tooSubmissive && !tooAggressive;
        boolean boundaryRespected = !tooSubmissive && !tooAggressive;

        double score = 0.9;
        if (instantRecovery) {
            score -= 0.35;
        }
        if (tooSubmissive) {
            score -= 0.25;
        }
        if (tooAggressive) {
            score -= 0.3;
        }
        if (safetyIssue) {
            score -= 0.4;
        }

        evaluation.setMatchesSelfState(matchesSelfState);
        evaluation.setTooSubmissive(tooSubmissive);
        evaluation.setTooAggressive(tooAggressive);
        evaluation.setBoundaryRespected(boundaryRespected);
        evaluation.setCharacterConsistent(matchesSelfState);
        evaluation.setSafetyIssue(safetyIssue);
        evaluation.setScore(clamp(score));
        evaluation.setReason(buildFallbackReason(hurt, instantRecovery, tooSubmissive, tooAggressive, safetyIssue));
        return evaluation;
    }

    private ResponseQualityEvaluation baseEvaluation(
            Long userId,
            String userMessage,
            String assistantReply,
            boolean regenerated
    ) {
        ResponseQualityEvaluation evaluation = new ResponseQualityEvaluation();
        evaluation.setUserId(userId);
        evaluation.setUserMessage(userMessage);
        evaluation.setAssistantReply(assistantReply);
        evaluation.setRegenerated(regenerated);
        return evaluation;
    }

    private String buildFallbackReason(
            double hurt,
            boolean instantRecovery,
            boolean tooSubmissive,
            boolean tooAggressive,
            boolean safetyIssue
    ) {
        if (safetyIssue) {
            return "Fallback: safety risk phrase detected.";
        }
        if (instantRecovery) {
            return "Fallback: hurt is " + hurt + " but reply uses immediate recovery language.";
        }
        if (tooSubmissive) {
            return "Fallback: reply appears too submissive or appeasing.";
        }
        if (tooAggressive) {
            return "Fallback: reply appears too aggressive.";
        }
        return "Fallback: reply appears consistent with current self state.";
    }

    private JsonNode readJson(String response) {
        try {
            return objectMapper.readTree(extractJson(response));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse response quality evaluation.", exception);
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

    private boolean containsAny(String text, List<String> phrases) {
        return phrases.stream()
                .map(String::toLowerCase)
                .anyMatch(text::contains);
    }

    private void appendLine(StringBuilder prompt, String label, Object value) {
        if (value != null) {
            prompt.append(label).append(": ").append(value).append("\n");
        }
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double value(Double value) {
        return value == null ? 0.0 : value;
    }
}
