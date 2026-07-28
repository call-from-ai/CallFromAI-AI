package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.ChatHistoryItem;
import com.example.aidatingagentbackend.entity.Memory;
import com.example.aidatingagentbackend.entity.MemoryChannel;
import com.example.aidatingagentbackend.entity.MemoryType;
import com.example.aidatingagentbackend.repository.MemoryRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationMemoryServiceTests {

    @Test
    void combinesChatAndCallTurnsInChronologicalPromptOrder() {
        MemoryRepository repository = mock(MemoryRepository.class);
        ConversationMemoryService service = new ConversationMemoryService(repository);
        Memory call = turn(2L, MemoryChannel.CALL, "전화 사용자", "전화 AI");
        Memory chat = turn(1L, MemoryChannel.CHAT, "채팅 사용자", "채팅 AI");
        when(repository.findTop5ByCharacterIdAndTypeOrderByOccurredAtDescIdDesc(
                10L, MemoryType.CONVERSATION_TURN
        )).thenReturn(List.of(call, chat));

        List<ChatHistoryItem> history = service.findRecentHistory(10L);

        assertThat(history).extracting(ChatHistoryItem::role, ChatHistoryItem::content)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("user-chat", "채팅 사용자"),
                        org.assertj.core.groups.Tuple.tuple("assistant-chat", "채팅 AI"),
                        org.assertj.core.groups.Tuple.tuple("user-call", "전화 사용자"),
                        org.assertj.core.groups.Tuple.tuple("assistant-call", "전화 AI")
                );
    }

    @Test
    void savesOneConversationTurnPerRequestId() {
        MemoryRepository repository = mock(MemoryRepository.class);
        when(repository.findByRequestId("req-1")).thenReturn(Optional.empty());
        ConversationMemoryService service = new ConversationMemoryService(repository);

        service.saveTurn("req-1", 10L, MemoryChannel.CALL, "안녕", "반가워");

        verify(repository).save(any(Memory.class));
    }

    @Test
    void skipsDuplicateRequestId() {
        MemoryRepository repository = mock(MemoryRepository.class);
        when(repository.findByRequestId("req-1")).thenReturn(Optional.of(new Memory()));
        ConversationMemoryService service = new ConversationMemoryService(repository);

        service.saveTurn("req-1", 10L, MemoryChannel.CHAT, "안녕", "반가워");

        verify(repository, never()).save(any());
    }

    private Memory turn(Long id, MemoryChannel channel, String user, String assistant) {
        Memory memory = new Memory();
        memory.setId(id);
        memory.setCharacterId(10L);
        memory.setType(MemoryType.CONVERSATION_TURN);
        memory.setChannel(channel);
        memory.setUserContent(user);
        memory.setAssistantContent(assistant);
        memory.setOccurredAt(LocalDateTime.now());
        return memory;
    }
}
