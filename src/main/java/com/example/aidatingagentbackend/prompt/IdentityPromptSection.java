package com.example.aidatingagentbackend.prompt;

import com.example.aidatingagentbackend.dto.CharacterSnapshot;

public final class IdentityPromptSection {
    public void append(StringBuilder prompt, CharacterSnapshot character, PersonaPolicy policy) {
        if (character == null || policy == null) return;
        prompt.append("[Persona]\n");
        append(prompt, "Name", policy.name());
        append(prompt, "Core", first(policy.corePersona(), 120));
        append(prompt, "MBTI", policy.mbti());
        append(prompt, "Job", character.getJob());
        append(prompt, "LifeType", character.getLifeType());
        prompt.append("\n");
        if (policy.activeBehavior() != null && !policy.activeBehavior().isBlank()) {
            prompt.append("Active behavior: ").append(policy.activeBehavior()).append("\n");
            prompt.append("Show it observably in this reply when safe and relevant.\n");
        }
        if (!policy.supportingTendencies().isEmpty()) {
            prompt.append("Supporting: ").append(String.join(" / ", policy.supportingTendencies())).append("\n");
        }
        prompt.append("Selected behavior defines visible identity; traits tune intensity without erasing it.\n\n");
    }

    private void append(StringBuilder prompt, String label, Object value) {
        if (value != null && !value.toString().isBlank()) prompt.append(label).append("=").append(value).append(" ");
    }

    private String first(String value, int limit) {
        if (value == null || value.isBlank()) return null;
        String stripped = value.strip();
        int newline = stripped.indexOf('\n');
        if (newline >= 0) stripped = stripped.substring(0, newline).strip();
        return stripped.length() <= limit ? stripped : stripped.substring(0, limit).strip();
    }
}
