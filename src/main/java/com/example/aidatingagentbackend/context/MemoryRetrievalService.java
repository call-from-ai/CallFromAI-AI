package com.example.aidatingagentbackend.context;

import com.example.aidatingagentbackend.entity.Memory;
import com.example.aidatingagentbackend.entity.State;
import com.example.aidatingagentbackend.repository.MemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MemoryRetrievalService {

    private static final int MAX_RETRIEVED_MEMORIES = 5;

    private final MemoryRepository memoryRepository;

    public MemoryRetrievalService(MemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    public List<Memory> retrieve(String userMessage, State state) {
        String currentEmotion = state == null ? null : state.getEmotion();
        Set<String> queryTerms = tokenize(userMessage);

        return memoryRepository.findAll()
                .stream()
                .sorted(Comparator.comparingInt(memory -> -score(memory, queryTerms, currentEmotion)))
                .limit(MAX_RETRIEVED_MEMORIES)
                .toList();
    }

    private int score(Memory memory, Set<String> queryTerms, String currentEmotion) {
        int score = memory.getImportance() == null ? 0 : memory.getImportance();
        String summary = memory.getSummary();

        if (StringUtils.hasText(currentEmotion)
                && StringUtils.hasText(summary)
                && summary.toLowerCase().contains(currentEmotion.toLowerCase())) {
            score += 5;
        }

        Set<String> memoryTerms = tokenize(summary);
        for (String term : queryTerms) {
            if (memoryTerms.contains(term)) {
                score += 2;
            }
        }

        return score;
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
