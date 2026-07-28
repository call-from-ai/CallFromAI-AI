package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.ChatHistoryItem;
import com.example.aidatingagentbackend.entity.Memory;
import com.example.aidatingagentbackend.entity.MemoryChannel;
import com.example.aidatingagentbackend.entity.MemoryType;
import com.example.aidatingagentbackend.repository.MemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ConversationMemoryService {

    private final MemoryRepository memoryRepository;

    public ConversationMemoryService(MemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    @Transactional(readOnly = true)
    public List<ChatHistoryItem> findRecentHistory(Long characterId) {
        if (characterId == null) {
            return List.of();
        }

        List<Memory> recent = new ArrayList<>(
                memoryRepository.findTop5ByCharacterIdAndTypeOrderByOccurredAtDescIdDesc(
                        characterId,
                        MemoryType.CONVERSATION_TURN
                )
        );
        Collections.reverse(recent);

        List<ChatHistoryItem> history = new ArrayList<>(recent.size() * 2);
        for (Memory memory : recent) {
            String channel = memory.getChannel() == null ? "" : "-" + memory.getChannel().name().toLowerCase();
            if (StringUtils.hasText(memory.getUserContent())) {
                history.add(new ChatHistoryItem("user" + channel, memory.getUserContent()));
            }
            if (StringUtils.hasText(memory.getAssistantContent())) {
                history.add(new ChatHistoryItem("assistant" + channel, memory.getAssistantContent()));
            }
        }
        return List.copyOf(history);
    }

    @Transactional
    public void saveTurn(
            String requestId,
            Long characterId,
            MemoryChannel channel,
            String userContent,
            String assistantContent
    ) {
        if (!StringUtils.hasText(requestId)
                || characterId == null
                || channel == null
                || !StringUtils.hasText(userContent)
                || !StringUtils.hasText(assistantContent)) {
            return;
        }
        if (memoryRepository.findByRequestId(requestId).isPresent()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Memory memory = new Memory();
        memory.setRequestId(requestId.trim());
        memory.setCharacterId(characterId);
        memory.setType(MemoryType.CONVERSATION_TURN);
        memory.setChannel(channel);
        memory.setUserContent(userContent);
        memory.setAssistantContent(assistantContent);
        memory.setOccurredAt(now);
        memory.setCreatedAt(now);
        memory.setRetrievalCount(0);
        memoryRepository.save(memory);
    }
}
