package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import org.springframework.stereotype.Service;

@Service
public class ResponseStylePostProcessor {

    public String process(String reply, RelationshipTemperature relationshipTemperature) {
        if (reply == null || reply.isBlank()) {
            return reply;
        }

        RelationshipTemperature temperature = relationshipTemperature == null
                ? RelationshipTemperature.NEUTRAL
                : relationshipTemperature;

        String processed = stripExcessivePeriods(reply);
        if (temperature == RelationshipTemperature.SPICY) {
            processed = spicyPolish(processed);
        }

        return processed.strip();
    }

    private String stripExcessivePeriods(String reply) {
        String[] lines = reply.split("\\R", -1);
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            String processedLine = line.stripTrailing();
            processedLine = processedLine.replaceAll("(?<=[가-힣A-Za-z0-9ㅋㅋㅎㅎㅠ])\\.$", "");
            builder.append(processedLine).append("\n");
        }
        return builder.toString().stripTrailing();
    }

    private String spicyPolish(String reply) {
        String processed = reply;
        processed = processed.replace("습니다", "");
        processed = processed.replace("해요", "해");
        processed = processed.replace("이에요", "임");
        processed = processed.replace("예요", "임");
        processed = processed.replace("그렇구나", "그래?");
        processed = processed.replace("괜찮았어?", "괜찮았냐");
        processed = processed.replace("뭐하고 있어?", "머함");
        return processed;
    }
}
