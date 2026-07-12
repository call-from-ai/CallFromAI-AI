package com.example.aidatingagentbackend.engine;

import com.example.aidatingagentbackend.entity.MemoryType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MemoryEngine {

    private static final int MIN_IMPORTANCE = 0;
    private static final int MAX_IMPORTANCE = 10;
    private static final int EPISODE_THRESHOLD = 7;
    private static final int SUMMARY_MAX_LENGTH = 200;

    private static final List<String> IMPORTANT_EVENT_KEYWORDS = List.of(
            "헤어지자",
            "이별",
            "배신",
            "고백",
            "좋아해",
            "사랑해",
            "첫 데이트",
            "기념일",
            "사과",
            "화해",
            "싸웠",
            "질투",
            "약속",
            "선물",
            "눈물"
    );

    private static final List<String> IMPORTANT_EMOTIONS = List.of(
            "sadness",
            "hurt",
            "happy",
            "jealousy",
            "anxiety",
            "relieved"
            , "upset"
            , "distant"
            , "affectionate"
            , "softened"
            , "hurt_but_listening"
    );

    public MemoryDecision analyze(String conversation, String emotion, Integer importance) {
        int normalizedImportance = clamp(importance);
        boolean importantConversation = containsImportantEvent(conversation);
        boolean importantEmotion = isImportantEmotion(emotion);
        int finalImportance = calculateImportance(normalizedImportance, importantConversation, importantEmotion);

        if (finalImportance < EPISODE_THRESHOLD) {
            return new MemoryDecision(false, null, finalImportance, null);
        }

        return new MemoryDecision(
                true,
                MemoryType.EPISODE,
                finalImportance,
                summarizeEpisode(conversation, emotion)
        );
    }

    private int calculateImportance(int importance, boolean importantConversation, boolean importantEmotion) {
        int score = importance;
        if (importantConversation) {
            score += 2;
        }
        if (importantEmotion) {
            score += 1;
        }

        return clamp(score);
    }

    private boolean containsImportantEvent(String conversation) {
        if (conversation == null || conversation.isBlank()) {
            return false;
        }

        String normalizedConversation = conversation.toLowerCase();
        return IMPORTANT_EVENT_KEYWORDS.stream()
                .map(String::toLowerCase)
                .anyMatch(normalizedConversation::contains);
    }

    private boolean isImportantEmotion(String emotion) {
        if (emotion == null || emotion.isBlank()) {
            return false;
        }

        String normalizedEmotion = emotion.toLowerCase();
        return IMPORTANT_EMOTIONS.stream()
                .map(String::toLowerCase)
                .anyMatch(normalizedEmotion::equals);
    }

    private String summarizeEpisode(String conversation, String emotion) {
        String summary = conversation == null ? "" : conversation.strip();
        if (summary.length() > SUMMARY_MAX_LENGTH) {
            summary = summary.substring(0, SUMMARY_MAX_LENGTH).strip() + "...";
        }

        if (emotion == null || emotion.isBlank()) {
            return summary;
        }

        return "Emotion=" + emotion + " | " + summary;
    }

    private int clamp(Integer value) {
        if (value == null) {
            return MIN_IMPORTANCE;
        }

        return Math.max(MIN_IMPORTANCE, Math.min(MAX_IMPORTANCE, value));
    }

    public record MemoryDecision(
            Boolean shouldCreate,
            MemoryType memoryType,
            Integer importance,
            String episodeSummary
    ) {
    }
}
