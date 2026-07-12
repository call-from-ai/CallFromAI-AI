package com.example.aidatingagentbackend.context;

import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import com.example.aidatingagentbackend.entity.Memory;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import com.example.aidatingagentbackend.entity.State;
import com.example.aidatingagentbackend.repository.MemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class MemoryRetrievalService {

    private static final int MAX_RETRIEVED_MEMORIES = 5;
    private static final double MAX_TRAIT_BONUS = 6.0;
    private static final double MAX_STAGE_BONUS = 2.0;

    private final MemoryRepository memoryRepository;
    private final MemoryEmbeddingService memoryEmbeddingService;

    public MemoryRetrievalService(
            MemoryRepository memoryRepository,
            MemoryEmbeddingService memoryEmbeddingService
    ) {
        this.memoryRepository = memoryRepository;
        this.memoryEmbeddingService = memoryEmbeddingService;
    }

    @Transactional
    public List<Memory> retrieve(Long characterId, String userMessage, State state) {
        return retrieve(characterId, userMessage, state, null, null, null, null);
    }

    @Transactional
    public List<Memory> retrieve(
            Long characterId,
            String userMessage,
            State state,
            CharacterTraitProfile characterTraitProfile,
            RelationshipStage relationshipStage,
            Integer relationshipTemperatureScore,
            EventAnalysis eventAnalysis
    ) {
        String currentEmotion = state == null ? null : state.getEmotion();
        Set<String> queryTerms = tokenize(userMessage);
        double[] queryEmbedding = memoryEmbeddingService.embed(userMessage);

        List<Memory> source = characterId == null
                ? memoryRepository.findAll()
                : memoryRepository.findByCharacterId(characterId);

        List<Memory> retrieved = source
                .stream()
                .sorted(Comparator.comparingDouble(memory -> -score(
                        memory,
                        queryTerms,
                        queryEmbedding,
                        currentEmotion,
                        characterTraitProfile,
                        relationshipStage,
                        eventAnalysis
                )))
                .limit(MAX_RETRIEVED_MEMORIES)
                .toList();
        markRetrieved(retrieved);
        return retrieved;
    }

    private double score(
            Memory memory,
            Set<String> queryTerms,
            double[] queryEmbedding,
            String currentEmotion,
            CharacterTraitProfile characterTraitProfile,
            RelationshipStage relationshipStage,
            EventAnalysis eventAnalysis
    ) {
        double score = memory.getImportance() == null ? 0.0 : memory.getImportance() * 0.5;
        String summary = memory.getSummary();
        double[] memoryEmbedding = resolveEmbedding(memory);
        score += memoryEmbeddingService.cosineSimilarity(queryEmbedding, memoryEmbedding) * 70.0;

        if (StringUtils.hasText(currentEmotion)
                && StringUtils.hasText(summary)
                && summary.toLowerCase().contains(currentEmotion.toLowerCase())) {
            score += 8.0;
        }

        Set<String> memoryTerms = tokenize(summary);
        for (String term : queryTerms) {
            if (memoryTerms.contains(term)) {
                score += 1.0;
            }
        }

        score -= recentUsePenalty(memory);
        score += traitBonus(memory, summary, queryTerms, characterTraitProfile, eventAnalysis);
        score += stageBonus(memory, relationshipStage);

        return score;
    }

    private double traitBonus(
            Memory memory,
            String summary,
            Set<String> queryTerms,
            CharacterTraitProfile traits,
            EventAnalysis eventAnalysis
    ) {
        if (!StringUtils.hasText(summary) || traits == null) {
            return 0.0;
        }

        String text = summary.toLowerCase();
        double bonus = 0.0;
        if (high(traits.getAttachment())
                && matchesAny(text, "약속", "연락", "답장", "갈등", "화해", "보고 싶")) {
            bonus += 1.5;
        }
        if (high(traits.getEmpathy())
                && matchesAny(text, "고민", "피곤", "힘들", "슬픔", "우울", "스트레스", "불안")) {
            bonus += 1.5;
        }
        if (high(traits.getJealousy())
                && isJealousyEvent(eventAnalysis)
                && matchesAny(text, "질투", "다른 사람", "전 애인", "전남친", "전여친")) {
            bonus += 1.5;
        }
        if (high(traits.getAffection())
                && matchesAny(text, "기념일", "데이트", "선호", "좋아", "함께", "같이", "선물")) {
            bonus += 1.5;
        }

        if (!hasWeakRelevance(summary, queryTerms)) {
            bonus *= 0.5;
        }
        return Math.min(MAX_TRAIT_BONUS, bonus);
    }

    private double stageBonus(Memory memory, RelationshipStage relationshipStage) {
        if (relationshipStage != RelationshipStage.LONG_TERM) {
            return 0.0;
        }
        int importance = memory.getImportance() == null ? 0 : memory.getImportance();
        if (importance < 7 || memory.getCreatedAt() == null) {
            return 0.0;
        }

        long days = Duration.between(memory.getCreatedAt(), LocalDateTime.now()).toDays();
        return days >= 14 ? MAX_STAGE_BONUS : 0.0;
    }

    private boolean hasWeakRelevance(String summary, Set<String> queryTerms) {
        if (queryTerms == null || queryTerms.isEmpty()) {
            return true;
        }
        Set<String> memoryTerms = tokenize(summary);
        return queryTerms.stream().anyMatch(memoryTerms::contains);
    }

    private boolean isJealousyEvent(EventAnalysis eventAnalysis) {
        if (eventAnalysis == null) {
            return false;
        }
        String context = (eventAnalysis.primaryEmotion() == null ? "" : eventAnalysis.primaryEmotion().toLowerCase())
                + " "
                + (eventAnalysis.summary() == null ? "" : eventAnalysis.summary().toLowerCase());
        return context.contains("jealous")
                || context.contains("질투")
                || context.contains("다른 사람")
                || context.contains("전 애인")
                || eventAnalysis.eventType() == AgentEventType.IGNORE_OR_COLD && context.contains("경쟁");
    }

    private boolean high(Integer value) {
        return value != null && value >= 8;
    }

    private boolean matchesAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private double recentUsePenalty(Memory memory) {
        if (memory.getLastRetrievedAt() == null) {
            return 0.0;
        }

        long minutes = Duration.between(memory.getLastRetrievedAt(), LocalDateTime.now()).toMinutes();
        double recencyPenalty = minutes < 10 ? 18.0 : minutes < 30 ? 10.0 : minutes < 120 ? 4.0 : 0.0;
        int retrievalCount = memory.getRetrievalCount() == null ? 0 : memory.getRetrievalCount();
        return recencyPenalty + Math.min(8.0, retrievalCount * 0.8);
    }

    private void markRetrieved(List<Memory> memories) {
        LocalDateTime now = LocalDateTime.now();
        for (Memory memory : memories) {
            memory.setLastRetrievedAt(now);
            memory.setRetrievalCount((memory.getRetrievalCount() == null ? 0 : memory.getRetrievalCount()) + 1);
        }
        memoryRepository.saveAll(memories);
    }

    private double[] resolveEmbedding(Memory memory) {
        double[] embedding = memoryEmbeddingService.deserialize(memory.getEmbedding());
        if (embedding != null) {
            return embedding;
        }

        double[] computed = memoryEmbeddingService.embed(memory.getSummary());
        memory.setEmbedding(memoryEmbeddingService.serialize(computed));
        memoryRepository.save(memory);
        return computed;
    }

    private Set<String> tokenize(String text) {
        if (!StringUtils.hasText(text)) {
            return Set.of();
        }

        return Arrays.stream(text.toLowerCase().split("[^a-z0-9가-힣]+"))
                .filter(token -> token.length() >= 2)
                .collect(Collectors.toSet());
    }
}
