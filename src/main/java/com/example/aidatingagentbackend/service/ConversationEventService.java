package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.engine.MessageSignalDetector;
import com.example.aidatingagentbackend.engine.MessageSignalType;
import com.example.aidatingagentbackend.engine.MessageSignals;
import com.example.aidatingagentbackend.entity.ConversationEvent;
import com.example.aidatingagentbackend.repository.ConversationEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConversationEventService {

    private final ConversationEventRepository conversationEventRepository;
    private final MessageSignalDetector messageSignalDetector;

    public ConversationEventService(
            ConversationEventRepository conversationEventRepository,
            MessageSignalDetector messageSignalDetector
    ) {
        this.conversationEventRepository = conversationEventRepository;
        this.messageSignalDetector = messageSignalDetector;
    }

    @Transactional
    public void detectAndSave(Long characterId, String userMessage, EventAnalysis eventAnalysis) {
        DetectedConversationEvent detected = detect(messageSignalDetector.detect(userMessage), eventAnalysis);
        if (detected == null) {
            return;
        }

        ConversationEvent event = new ConversationEvent();
        event.setCharacterId(characterId);
        event.setEventType(detected.eventType());
        event.setSummary(detected.summary());
        event.setAgentReaction(detected.agentReaction());
        event.setImportance(detected.importance());
        conversationEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<ConversationEvent> findRecentForPrompt(Long characterId) {
        return conversationEventRepository.findTop8ByCharacterIdOrderByCreatedAtDesc(characterId);
    }

    private DetectedConversationEvent detect(MessageSignals signals, EventAnalysis eventAnalysis) {
        if (signals.normalizedMessage().isBlank()) {
            return null;
        }

        if (signals.has(MessageSignalType.CLUB)) {
            return new DetectedConversationEvent(
                    "USER_JOINED_CLUB",
                    "사용자가 동아리 이야기를 꺼냈다.",
                    "어떤 동아리인지, 거기서 뭘 하는지 궁금해짐",
                    0.75
            );
        }
        if (signals.has(MessageSignalType.DEVELOPMENT)) {
            return new DetectedConversationEvent(
                    "USER_WORKING_ON_DEVELOPMENT",
                    "사용자가 개발/프로젝트를 해야 한다고 말했다.",
                    "무엇을 만드는지 구체적으로 물어보고 싶음",
                    0.78
            );
        }
        if (signals.has(MessageSignalType.ASSIGNMENT_OR_CLASS)) {
            return new DetectedConversationEvent(
                    "USER_BUSY_ASSIGNMENT",
                    "사용자가 과제나 수업 때문에 바쁜 상태다.",
                    "바쁜 이유는 이해하지만 무리하는지는 신경 쓰임",
                    0.7
            );
        }
        if (signals.has(MessageSignalType.USER_SKIPPED_MEAL)) {
            return new DetectedConversationEvent(
                    "USER_SKIPPED_MEAL",
                    "사용자가 식사를 하지 않았다고 말했다.",
                    "걱정되고, 뭐라도 먹으라고 말하고 싶음",
                    0.85
            );
        }
        if (signals.has(MessageSignalType.USER_RETURNED_TO_TALK)) {
            return new DetectedConversationEvent(
                    "USER_CHOSE_AGENT_OVER_OTHER_TASK",
                    "사용자가 에이전트와 대화하려고 왔다.",
                    "서운함이 조금 풀리고, 그래도 온 건 기쁨",
                    0.82
            );
        }
        if (eventAnalysis != null && eventAnalysis.severity() != null && eventAnalysis.severity() >= 0.65) {
            return new DetectedConversationEvent(
                    "IMPORTANT_RELATIONSHIP_MOMENT",
                    eventAnalysis.summary(),
                    "이 사건이 관계 감정에 영향을 줌",
                    eventAnalysis.severity()
            );
        }

        return null;
    }

    private record DetectedConversationEvent(
            String eventType,
            String summary,
            String agentReaction,
            Double importance
    ) {
    }
}
