package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.entity.MemoryChannel;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResponseSanitizer {
    private static final String CALL_FALLBACK_REPLY = "응, 듣고 있어";
    private static final Pattern GRAPHEME = Pattern.compile("\\X");

    public String sanitize(String reply, MemoryChannel channel) {
        if (reply == null) return null;
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
        Matcher matcher = GRAPHEME.matcher(value);
        while (matcher.find()) {
            String grapheme = matcher.group();
            if (isEmojiSequence(grapheme)) {
                if (++emojiCount <= maxEmoji) result.append(grapheme);
            } else result.append(grapheme);
        }
        return normalizeSpaces(result.toString());
    }

    String stripEmoji(String value) {
        StringBuilder result = new StringBuilder(value.length());
        Matcher matcher = GRAPHEME.matcher(value);
        while (matcher.find()) {
            String grapheme = matcher.group();
            if (!isEmojiSequence(grapheme)) result.append(grapheme);
        }
        return normalizeSpaces(result.toString());
    }

    private String normalizeSpaces(String value) {
        return value.replaceAll("[ \\t]{2,}", " ").strip();
    }

    private boolean isEmojiCodePoint(int codePoint) {
        return isEmojiBase(codePoint) || (codePoint >= 0x1F3FB && codePoint <= 0x1F3FF)
                || codePoint == 0x200D || codePoint == 0xFE0F || codePoint == 0x20E3;
    }

    private boolean isEmojiSequence(String grapheme) {
        int[] codePoints = grapheme.codePoints().toArray();
        boolean regionalPair = codePoints.length == 2 && isRegionalIndicator(codePoints[0]) && isRegionalIndicator(codePoints[1]);
        boolean keycap = grapheme.indexOf(0x20E3) >= 0;
        if (regionalPair || keycap) return true;
        for (int codePoint : codePoints) if (isEmojiCodePoint(codePoint)) return true;
        return false;
    }

    private boolean isRegionalIndicator(int codePoint) {
        return codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF;
    }

    private boolean isEmojiBase(int codePoint) {
        return (codePoint >= 0x1F000 && codePoint <= 0x1FAFF)
                || (codePoint >= 0x2600 && codePoint <= 0x27BF)
                || (codePoint >= 0x2300 && codePoint <= 0x23FF)
                || (codePoint >= 0x2B00 && codePoint <= 0x2BFF)
                || (codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF);
    }
}
