package com.example.aidatingagentbackend.prompt;

import com.example.aidatingagentbackend.entity.MemoryChannel;

public final class PromptContextSelector {
    public Selection select(String userMessage, MemoryChannel channel, boolean hasPersonaBehavior) {
        String text = normalize(userMessage);
        boolean schedule = containsAny(text, "주말", "시간", "몇 시", "뭐 해", "뭐해", "일정", "계획", "약속", "출근", "퇴근", "학교", "쉬어");
        boolean preference = containsAny(text, "좋아", "싫어", "취향", "먹고 싶", "보고 싶", "뭐 먹", "어떤 게", "골라");
        boolean past = containsAny(text, "기억", "전에", "예전에", "지난", "그때", "우리", "말했", "잊었");
        boolean life = schedule || containsAny(text, "회사", "직장", "수업", "과제", "공부", "일은", "바빠");
        return new Selection(schedule, life, preference, past, past,
                !hasPersonaBehavior, channel == MemoryChannel.CHAT);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase().replaceAll("\\s+", " ").strip();
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) if (text.contains(candidate)) return true;
        return false;
    }

    public record Selection(boolean timeDetail, boolean life, boolean preference,
                            boolean memory, boolean sharedEvents, boolean styleExamples,
                            boolean conversationPlans) {
    }
}
