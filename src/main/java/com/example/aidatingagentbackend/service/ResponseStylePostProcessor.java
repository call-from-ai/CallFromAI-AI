package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.dto.CharacterTraitSnapshot;
import com.example.aidatingagentbackend.dto.RelationshipStrategy;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import com.example.aidatingagentbackend.entity.MemoryChannel;
import org.springframework.stereotype.Service;

@Service
public class ResponseStylePostProcessor {

    private static final String CALL_FALLBACK_REPLY = "응, 듣고 있어";

    public String process(String reply, MemoryChannel channel, RelationshipStrategy strategy, Integer relationshipTemperatureScore,
            Integer romanceStyleScore, CharacterTraitSnapshot traits, RelationshipStage stage, AgentSelfState selfState) {
        if (reply == null || reply.isBlank()) {
            return reply;
        }

        int score = relationshipTemperatureScore == null ? 50
                : Math.max(0, Math.min(100, relationshipTemperatureScore));

        String processed = stripExcessivePeriods(reply);
        if (strategy == RelationshipStrategy.CONFLICT_REPAIR) {
            processed = conflictRepairPolish(processed);
        }
        processed = scoreBasedPolish(processed, score, romanceStyleScore, traits, stage, selfState);
        if (channel == MemoryChannel.CALL) {
            processed = stripEmoji(processed);
            if (processed.isBlank()) {
                processed = CALL_FALLBACK_REPLY;
            }
        }

        return processed.strip();
    }

    String stripEmoji(String value) {
        StringBuilder result = new StringBuilder(value.length());
        value.codePoints().forEach(codePoint -> {
            if (!isEmojiCodePoint(codePoint)) result.appendCodePoint(codePoint);
        });
        return result.toString().replaceAll("[ \\t]{2,}", " ").strip();
    }

    private boolean isEmojiCodePoint(int codePoint) {
        return (codePoint >= 0x1F000 && codePoint <= 0x1FAFF)
                || (codePoint >= 0x2600 && codePoint <= 0x27BF)
                || (codePoint >= 0x2300 && codePoint <= 0x23FF)
                || (codePoint >= 0x2B00 && codePoint <= 0x2BFF)
                || (codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF)
                || (codePoint >= 0x1F3FB && codePoint <= 0x1F3FF)
                || codePoint == 0x200D || codePoint == 0xFE0F || codePoint == 0x20E3;
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

    private String conflictRepairPolish(String reply) {
        String processed = reply;
        processed = processed.replace("괜찮아, 다행이야", "말은 들을게");
        processed = processed.replace("고마워.", "고마워");
        return processed;
    }

    private String scoreBasedPolish(
            String reply,
            int temperatureScore,
            Integer romanceStyleScore,
            CharacterTraitSnapshot traits,
            RelationshipStage stage,
            AgentSelfState selfState
    ) {
        String processed = reply;
        int styleScore = romanceStyleScore == null ? 50 : Math.max(0, Math.min(100, romanceStyleScore));
        if (styleScore >= 81) {
            processed = trimQuestionPileup(processed);
            processed = reducePeriods(processed);
        } else if (styleScore <= 20) {
            processed = limitLaughs(processed, 1);
        }

        if (traits != null && value(traits.getExpressiveness()) <= 2) {
            processed = limitExclamation(processed);
        }
        if (stage == RelationshipStage.CRUSH) {
            processed = processed.replace("자기야", "너");
            processed = processed.replace("내 사랑", "너");
        }
        if (selfState != null && low(selfState.getAnger())) {
            processed = processed.replace("짜증나", "좀 그렇네");
        }
        return processed;
    }

    private String trimQuestionPileup(String reply) {
        String[] lines = reply.split("\\R", -1);
        int questionCount = 0;
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            String processedLine = line;
            if (processedLine.stripTrailing().endsWith("?")) {
                questionCount++;
                if (questionCount > 1) {
                    processedLine = processedLine.replaceAll("\\?+$", "");
                }
            }
            builder.append(processedLine).append("\n");
        }
        return builder.toString().stripTrailing();
    }

    private String reducePeriods(String reply) {
        return reply.replaceAll("(?<=[가-힣A-Za-z0-9ㅋㅋㅎㅎㅠ])\\.(?=\\s|$)", "");
    }

    private String softenFormalEndings(String reply) {
        String processed = reply;
        processed = processed.replace("합니다", "해");
        processed = processed.replace("했어요", "했어");
        processed = processed.replace("할게요", "할게");
        return processed;
    }

    private String limitLaughs(String reply, int max) {
        String processed = reply;
        int first = processed.indexOf("ㅋㅋ");
        if (first < 0) {
            return processed;
        }
        int count = 0;
        StringBuilder builder = new StringBuilder();
        int index = 0;
        while (index < processed.length()) {
            if (processed.startsWith("ㅋㅋ", index)) {
                count++;
                if (count <= max) {
                    builder.append("ㅋㅋ");
                }
                index += 2;
            } else {
                builder.append(processed.charAt(index));
                index++;
            }
        }
        return builder.toString();
    }

    private String limitExclamation(String reply) {
        return reply.replaceAll("!{2,}", "!");
    }

    private int value(Integer value) {
        return value == null ? 5 : Math.max(0, Math.min(10, value));
    }

    private boolean low(Double value) {
        return value == null || value < 0.3;
    }
}

