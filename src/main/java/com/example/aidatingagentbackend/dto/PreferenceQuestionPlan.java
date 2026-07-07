package com.example.aidatingagentbackend.dto;

public record PreferenceQuestionPlan(
        String questionType,
        String preferenceKey,
        String action,
        String knownPreference,
        String inventionHint,
        String constraint
) {

    public boolean active() {
        return questionType != null && !"NONE".equals(questionType);
    }
}
