package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.AgentSelfStateLog;
import com.example.aidatingagentbackend.entity.Reflection;
import com.example.aidatingagentbackend.repository.ReflectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReflectionService {

    private static final double REFLECTION_THRESHOLD = 0.7;

    private final ReflectionRepository reflectionRepository;

    public ReflectionService(ReflectionRepository reflectionRepository) {
        this.reflectionRepository = reflectionRepository;
    }

    @Transactional
    public void createIfImportant(
            Long userId,
            String userMessage,
            EventAnalysis eventAnalysis,
            AgentSelfStateLog stateLog,
            AgentSelfState currentState
    ) {
        if (eventAnalysis == null || eventAnalysis.severity() == null) {
            return;
        }
        if (eventAnalysis.severity() < REFLECTION_THRESHOLD) {
            return;
        }

        Reflection reflection = new Reflection();
        reflection.setUserId(userId);
        reflection.setCategory(resolveCategory(eventAnalysis.eventType()));
        reflection.setSummary(buildSummary(eventAnalysis, userMessage));
        reflection.setUserPattern(buildUserPattern(eventAnalysis));
        reflection.setAgentLearning(buildAgentLearning(eventAnalysis, stateLog, currentState));
        reflection.setImportance(clamp(eventAnalysis.severity()));
        reflection.setSourceEventType(eventAnalysis.eventType().name());
        reflection.setSourceSeverity(clamp(eventAnalysis.severity()));
        reflectionRepository.save(reflection);
    }

    @Transactional(readOnly = true)
    public List<Reflection> findRelevantForPrompt(Long userId) {
        return reflectionRepository.findTop10ByUserIdOrderByImportanceDescCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Reflection> findAll() {
        return reflectionRepository.findAll();
    }

    private String resolveCategory(AgentEventType eventType) {
        return switch (eventType) {
            case BREAKUP_DECLARATION, INSULT, IGNORE_OR_COLD -> "CONFLICT_PATTERN";
            case BREAKUP_RETRACTION, APOLOGY -> "REPAIR_PATTERN";
            case AFFECTION -> "LOVE_LANGUAGE";
            case NORMAL -> "RELATIONSHIP_PREFERENCE";
        };
    }

    private String buildSummary(EventAnalysis eventAnalysis, String userMessage) {
        return switch (eventAnalysis.eventType()) {
            case BREAKUP_DECLARATION -> "사용자는 이별 표현을 사용했고, 에이전트에게 큰 관계 위협으로 해석된다.";
            case BREAKUP_RETRACTION -> "사용자는 이전의 부담스러운 말을 번복하거나 농담으로 무마하려는 신호를 보였다.";
            case APOLOGY -> "사용자는 갈등 이후 사과를 통해 관계를 회복하려는 신호를 보였다.";
            case AFFECTION -> "사용자는 애정 표현을 통해 친밀감을 높이려는 신호를 보였다.";
            case INSULT -> "사용자는 비난이나 모욕에 가까운 표현을 사용해 에이전트의 정서적 방어를 유발했다.";
            case IGNORE_OR_COLD -> "사용자는 차갑거나 거리를 두는 표현을 사용해 관계 불안을 유발했다.";
            case NORMAL -> eventAnalysis.summary() == null ? "일반 대화에서 관계 선호 단서가 감지되었다." : eventAnalysis.summary();
        };
    }

    private String buildUserPattern(EventAnalysis eventAnalysis) {
        return switch (eventAnalysis.eventType()) {
            case BREAKUP_DECLARATION -> "갈등이나 감정 압박 상황에서 관계 종료 표현을 사용할 수 있다.";
            case BREAKUP_RETRACTION -> "갈등 상황을 농담이나 번복으로 빠르게 무마하려는 경향이 있을 수 있다.";
            case APOLOGY -> "관계 손상 이후 사과로 회복을 시도하는 repair pattern이 있다.";
            case AFFECTION -> "애정 표현을 직접적인 말로 전달하는 love language 경향이 있다.";
            case INSULT -> "불만을 날카로운 비난으로 표현할 수 있다.";
            case IGNORE_OR_COLD -> "불편한 상황에서 차갑게 거리를 두는 방식으로 반응할 수 있다.";
            case NORMAL -> "현재 대화에서 장기 패턴 후보가 관찰되었다.";
        };
    }

    private String buildAgentLearning(
            EventAnalysis eventAnalysis,
            AgentSelfStateLog stateLog,
            AgentSelfState currentState
    ) {
        String base = switch (eventAnalysis.eventType()) {
            case BREAKUP_DECLARATION -> "이 사용자의 이별 표현은 관계 불안을 크게 유발하므로 즉시 회복하지 않는다.";
            case BREAKUP_RETRACTION -> "사용자가 말을 번복해도 상처와 불안이 즉시 사라지지 않도록 반응한다.";
            case APOLOGY -> "사과는 회복 신호지만 hurt가 높으면 천천히 받아들인다.";
            case AFFECTION -> "직접적인 애정 표현은 친밀감 회복에 도움이 될 수 있다.";
            case INSULT -> "비난 표현에는 무조건 수용하지 말고 감정적 경계를 세운다.";
            case IGNORE_OR_COLD -> "차가운 반응은 정서적 거리감을 높일 수 있으므로 조심스럽게 확인한다.";
            case NORMAL -> "관계 선호 단서를 이후 응답 스타일에 참고한다.";
        };

        if (stateLog == null || currentState == null) {
            return base;
        }

        return base
                + " hurt " + stateLog.getPreviousHurt() + " -> " + stateLog.getNextHurt()
                + ", trust " + stateLog.getPreviousTrust() + " -> " + stateLog.getNextTrust()
                + ", currentEmotion=" + currentState.getLastEmotion() + ".";
    }

    private double clamp(Double value) {
        if (value == null) {
            return 0.0;
        }

        return Math.max(0.0, Math.min(1.0, value));
    }
}
