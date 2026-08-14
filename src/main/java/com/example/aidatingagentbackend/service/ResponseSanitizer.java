package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.entity.MemoryChannel;
import org.springframework.stereotype.Service;

@Service
public class ResponseSanitizer {
    private static final String CALL_FALLBACK_REPLY = "응, 듣고 있어";

    public String sanitize(String reply, MemoryChannel channel) {
        if (reply == null || reply.isBlank()) return reply;
        String processed = reply.strip();
        if (channel == MemoryChannel.CALL) {
            processed = stripEmoji(processed);
            return processed.isBlank() ? CALL_FALLBACK_REPLY : processed;
        }
        return limitEmoji(processed, 1);
    }

    String limitEmoji(String value, int maxEmoji) {
        StringBuilder result = new StringBuilder(value.length());
        int emojiCount = 0;
        boolean keepingEmojiSequence = false;
        boolean joinedNext = false;
        for (int codePoint : value.codePoints().toArray()) {
            boolean emojiBase = isEmojiBase(codePoint);
            boolean emojiJoiner = codePoint == 0x200D || codePoint == 0xFE0F
                    || (codePoint >= 0x1F3FB && codePoint <= 0x1F3FF) || codePoint == 0x20E3;
            if (emojiBase) {
                if (!joinedNext) emojiCount++;
                keepingEmojiSequence = emojiCount <= maxEmoji;
                if (keepingEmojiSequence) result.appendCodePoint(codePoint);
                joinedNext = false;
            } else if (emojiJoiner) {
                if (keepingEmojiSequence) result.appendCodePoint(codePoint);
                joinedNext = codePoint == 0x200D;
            } else {
                keepingEmojiSequence = false;
                joinedNext = false;
                result.appendCodePoint(codePoint);
            }
        }
        return normalizeSpaces(result.toString());
    }

    String stripEmoji(String value) {
        StringBuilder result = new StringBuilder(value.length());
        value.codePoints().filter(codePoint -> !isEmojiCodePoint(codePoint)).forEach(result::appendCodePoint);
        return normalizeSpaces(result.toString());
    }

    private String normalizeSpaces(String value) {
        return value.replaceAll("[ \\t]{2,}", " ").strip();
    }

    private boolean isEmojiCodePoint(int codePoint) {
        return isEmojiBase(codePoint) || (codePoint >= 0x1F3FB && codePoint <= 0x1F3FF)
                || codePoint == 0x200D || codePoint == 0xFE0F || codePoint == 0x20E3;
    }

    private boolean isEmojiBase(int codePoint) {
        return (codePoint >= 0x1F000 && codePoint <= 0x1FAFF)
                || (codePoint >= 0x2600 && codePoint <= 0x27BF)
                || (codePoint >= 0x2300 && codePoint <= 0x23FF)
                || (codePoint >= 0x2B00 && codePoint <= 0x2BFF)
                || (codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF);
    }
}
