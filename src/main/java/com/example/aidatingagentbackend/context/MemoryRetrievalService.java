package com.example.aidatingagentbackend.context;

import com.example.aidatingagentbackend.entity.Memory;
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
        String currentEmotion = state == null ? null : state.getEmotion();
        Set<String> queryTerms = tokenize(userMessage);
        double[] queryEmbedding = memoryEmbeddingService.embed(userMessage);

        List<Memory> source = characterId == null
                ? memoryRepository.findAll()
                : memoryRepository.findByCharacterId(characterId);

        List<Memory> retrieved = source
                .stream()
                .sorted(Comparator.comparingDouble(memory -> -score(memory, queryTerms, queryEmbedding, currentEmotion)))
                .limit(MAX_RETRIEVED_MEMORIES)
                .toList();
        markRetrieved(retrieved);
        return retrieved;
    }

    private double score(Memory memory, Set<String> queryTerms, double[] queryEmbedding, String currentEmotion) {
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

        return score;
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
