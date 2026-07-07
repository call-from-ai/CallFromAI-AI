package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.entity.ConversationEvent;
import com.example.aidatingagentbackend.repository.ConversationEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConversationEventService {

    private final ConversationEventRepository conversationEventRepository;

    public ConversationEventService(ConversationEventRepository conversationEventRepository) {
        this.conversationEventRepository = conversationEventRepository;
    }

    @Transactional
    public void detectAndSave(Long userId, String userMessage, EventAnalysis eventAnalysis) {
        DetectedConversationEvent detected = detect(userMessage, eventAnalysis);
        if (detected == null) {
            return;
        }

        ConversationEvent event = new ConversationEvent();
        event.setUserId(userId);
        event.setEventType(detected.eventType());
        event.setSummary(detected.summary());
        event.setAgentReaction(detected.agentReaction());
        event.setImportance(detected.importance());
        conversationEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<ConversationEvent> findRecentForPrompt(Long userId) {
        return conversationEventRepository.findTop8ByUserIdOrderByCreatedAtDesc(userId);
    }

    private DetectedConversationEvent detect(String userMessage, EventAnalysis eventAnalysis) {
        String message = normalize(userMessage);
        if (message.isBlank()) {
            return null;
        }

        if (containsAny(message, "동아리")) {
            return new DetectedConversationEvent(
                    "USER_JOINED_CLUB",
                    "사용자가 동아리 이야기를 꺼냈다.",
                    "어떤 동아리인지, 거기서 뭘 하는지 궁금해짐",
                    0.75
            );
        }
        if (containsAny(message, "개발", "코딩", "앱", "백엔드", "프론트", "프로젝트")) {
            return new DetectedConversationEvent(
                    "USER_WORKING_ON_DEVELOPMENT",
                    "사용자가 개발/프로젝트를 해야 한다고 말했다.",
                    "무엇을 만드는지 구체적으로 물어보고 싶음",
                    0.78
            );
        }
        if (containsAny(message, "과제", "시험", "수업")) {
            return new DetectedConversationEvent(
                    "USER_BUSY_ASSIGNMENT",
                    "사용자가 과제나 수업 때문에 바쁜 상태다.",
                    "바쁜 이유는 이해하지만 무리하는지는 신경 쓰임",
                    0.7
            );
        }
        if (containsAny(message, "안 먹", "못 먹", "굶", "저녁 안", "밥 안")) {
            return new DetectedConversationEvent(
                    "USER_SKIPPED_MEAL",
                    "사용자가 식사를 하지 않았다고 말했다.",
                    "걱정되고, 뭐라도 먹으라고 말하고 싶음",
                    0.85
            );
        }
        if (containsAny(message, "너랑 얘기", "너랑 말", "얘기하려고", "말하려고", "보려고 왔", "왔다", "왔어")) {
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

    private boolean containsAny(String message, String... patterns) {
        for (String pattern : patterns) {
            if (message.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String message) {
        return message == null ? "" : message.toLowerCase();
    }

    private record DetectedConversationEvent(
            String eventType,
            String summary,
            String agentReaction,
            Double importance
    ) {
    }
}
