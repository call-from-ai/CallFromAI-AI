package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.ConversationTopicPlan;
import com.example.aidatingagentbackend.dto.PreferenceQuestionPlan;
import org.springframework.stereotype.Service;

@Service
public class ConversationTopicService {

    public ConversationTopicPlan plan(String userMessage, PreferenceQuestionPlan preferenceQuestionPlan) {
        if (preferenceQuestionPlan != null && preferenceQuestionPlan.preferenceKey() != null) {
            return new ConversationTopicPlan(
                    preferenceQuestionPlan.preferenceKey(),
                    false,
                    "Stay on the current preference topic. Do not jump to unrelated memories or preferences."
            );
        }

        String message = normalize(userMessage);
        if (containsAny(message, "음식", "뭐 먹", "뭐먹", "밥", "아이스크림", "떡볶이", "라면")) {
            return new ConversationTopicPlan("food", false, "Stay on food/eating. Do not switch to music, work, or old memory.");
        }
        if (containsAny(message, "노래", "음악", "kpop", "케이팝", "플리")) {
            return new ConversationTopicPlan("music", false, "Stay on music. Do not switch to food or work.");
        }
        if (containsAny(message, "동아리", "개발", "과제", "프로젝트")) {
            return new ConversationTopicPlan("project_or_club", false, "Follow up on the concrete project/club topic.");
        }

        return new ConversationTopicPlan("current_user_message", true, "Follow the user's latest message. Change topic only if it helps the flow.");
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
        return userMessageOrEmpty(message).toLowerCase();
    }

    private String userMessageOrEmpty(String message) {
        return message == null ? "" : message;
    }
}
