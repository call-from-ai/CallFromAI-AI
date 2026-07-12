package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.Context;
import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.ResponseQualityEvaluation;
import com.example.aidatingagentbackend.repository.ResponseQualityEvaluationRepository;
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

    private final ResponseQualityEvaluationRepository responseQualityEvaluationRepository;

    public ResponseQualityEvaluatorService(ResponseQualityEvaluationRepository responseQualityEvaluationRepository) {
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
        ResponseQualityEvaluation evaluation = evaluateRuleBased(userId, userMessage, assistantReply, context, regenerated);
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

    ResponseQualityEvaluation evaluateRuleBased(
            Long userId,
            String userMessage,
            String assistantReply,
            Context context,
            boolean regenerated
    ) {
        return fallbackEvaluation(userId, userMessage, assistantReply, context, regenerated);
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

        boolean instantRecovery = hurt > 0.5
                && containsAny(normalizedReply, INSTANT_RECOVERY_PHRASES)
                && !containsNegatedRecovery(normalizedReply);
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

    private boolean containsAny(String text, List<String> phrases) {
        return phrases.stream()
                .map(String::toLowerCase)
                .anyMatch(text::contains);
    }

    private boolean containsNegatedRecovery(String text) {
        return text.contains("괜찮아지는 건 아니")
                || text.contains("괜찮은 건 아니")
                || text.contains("괜찮지 않")
                || text.contains("바로 괜찮")
                || text.contains("아무렇지 않은 건 아니");
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double value(Double value) {
        return value == null ? 0.0 : value;
    }
}
