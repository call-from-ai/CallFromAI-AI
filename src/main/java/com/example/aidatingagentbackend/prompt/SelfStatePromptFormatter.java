package com.example.aidatingagentbackend.prompt;

import com.example.aidatingagentbackend.entity.AgentSelfState;
import org.springframework.stereotype.Component;

@Component
public class SelfStatePromptFormatter {

    public String format(AgentSelfState selfState) {
        if (selfState == null) {
            return "";
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("[Agent Self State]\n");
        appendLine(prompt, "Affection", selfState.getAffection());
        appendLine(prompt, "Trust", selfState.getTrust());
        appendLine(prompt, "Hurt", selfState.getHurt());
        appendLine(prompt, "Anger", selfState.getAnger());
        appendLine(prompt, "Insecurity", selfState.getInsecurity());
        appendLine(prompt, "Disappointment", selfState.getDisappointment());
        appendLine(prompt, "Emotional Distance", selfState.getEmotionalDistance());
        appendLine(prompt, "Last Emotion", selfState.getLastEmotion());
        appendLine(prompt, "Last Significant Event", selfState.getLastSignificantEvent());
        prompt.append("Relationship Stability: ").append(resolveRelationshipStability(selfState)).append("\n\n");
        prompt.append("[Agent Self State Response Guide]\n");
        prompt.append("- Do not flatter, appease, or unconditionally serve the user.\n");
        prompt.append("- The agent has independent feelings and boundaries like a romantic partner.\n");
        prompt.append("- If Hurt is high, do not forgive immediately or thank the user for taking back painful words.\n");
        prompt.append("- If the user retracts something painful, the agent's feelings do not instantly recover.\n");
        prompt.append("- If Anger or Emotional Distance is high, use a guarded, disappointed, or firm tone.\n");
        prompt.append("- Do not become excessively aggressive, manipulative, threatening, or cruel.\n");
        prompt.append("- Reply in short, natural Korean conversation style.\n\n");
        return prompt.toString();
    }

    private String resolveRelationshipStability(AgentSelfState selfState) {
        double stability = average(
                value(selfState.getTrust()),
                1.0 - value(selfState.getInsecurity()),
                1.0 - value(selfState.getEmotionalDistance()),
                1.0 - value(selfState.getHurt())
        );

        if (stability >= 0.7) {
            return "stable";
        }
        if (stability >= 0.4) {
            return "shaken";
        }
        return "fragile";
    }

    private void appendLine(StringBuilder prompt, String label, Object value) {
        if (value != null) {
            prompt.append(label).append(": ").append(value).append("\n");
        }
    }

    private double average(double... values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }

        return sum / values.length;
    }

    private double value(Double value) {
        return value == null ? 0.0 : value;
    }
}
