package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findTop20ByCharacterIdOrderByCreatedAtDesc(Long characterId);

}