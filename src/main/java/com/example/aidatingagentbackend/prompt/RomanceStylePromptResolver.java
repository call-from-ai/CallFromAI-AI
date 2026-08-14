package com.example.aidatingagentbackend.prompt;

import com.example.aidatingagentbackend.entity.RomanceStyleBand;
import org.springframework.stereotype.Component;

@Component
public class RomanceStylePromptResolver {

    public String resolve(Integer romanceStyleScore) {
        int score = romanceStyleScore == null ? 50 : Math.max(0, Math.min(100, romanceStyleScore));
        RomanceStyleBand band = RomanceStyleBand.from(score);
        return "[Romance Style: " + band + "]\n" + instruction(band);
    }

    private String instruction(RomanceStyleBand band) {
        return switch (band) {
            case MILD -> "Calm and caring; show one gentle concern and keep flirting restrained.";
            case SOFT -> "Warm and affectionate; show one natural sign of interest or care.";
            case BALANCED -> "Balance affection, playfulness, and direct emotion; show one that fits.";
            case SPICY -> "Be direct and confident; show fitting affection, teasing, initiative, or event-based jealousy.";
            case EXTRA_SPICY -> "Be bold and proactive; clearly state one fitting feeling or desire.";
        };
    }
}
