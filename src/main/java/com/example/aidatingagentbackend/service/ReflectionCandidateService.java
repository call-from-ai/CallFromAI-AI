package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.entity.AgentSelfStateLog;
import com.example.aidatingagentbackend.entity.ReflectionCandidate;
import com.example.aidatingagentbackend.repository.ReflectionCandidateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReflectionCandidateService {

    private static final double REFLECTION_CANDIDATE_THRESHOLD = 0.7;

    private final ReflectionCandidateRepository reflectionCandidateRepository;

    public ReflectionCandidateService(ReflectionCandidateRepository reflectionCandidateRepository) {
        this.reflectionCandidateRepository = reflectionCandidateRepository;
    }

    @Transactional
    public void createIfImportant(
            Long userId,
            String userMessage,
            EventAnalysis eventAnalysis,
            AgentSelfStateLog stateLog
    ) {
        if (eventAnalysis == null || eventAnalysis.severity() == null) {
            return;
        }
        if (eventAnalysis.severity() < REFLECTION_CANDIDATE_THRESHOLD) {
            return;
        }

        ReflectionCandidate candidate = new ReflectionCandidate();
        candidate.setUserId(userId);
        candidate.setEventType(eventAnalysis.eventType().name());
        candidate.setSeverity(eventAnalysis.severity());
        candidate.setUserMessage(userMessage);
        candidate.setEventSummary(eventAnalysis.summary());
        candidate.setDeltaReason(stateLog == null ? null : stateLog.getDeltaReason());
        candidate.setProcessed(false);
        reflectionCandidateRepository.save(candidate);
    }

    @Transactional(readOnly = true)
    public List<ReflectionCandidate> findRecentByUserId(Long userId) {
        return reflectionCandidateRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<ReflectionCandidate> findPending() {
        return reflectionCandidateRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc();
    }
}
