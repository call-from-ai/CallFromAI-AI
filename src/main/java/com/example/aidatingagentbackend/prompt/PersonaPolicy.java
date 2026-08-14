package com.example.aidatingagentbackend.prompt;

import java.util.List;

public record PersonaPolicy(
        String name,
        String corePersona,
        String mbti,
        String activeBehavior,
        List<String> supportingTendencies
) {
    public PersonaPolicy {
        supportingTendencies = supportingTendencies == null ? List.of() : List.copyOf(supportingTendencies);
    }
}
