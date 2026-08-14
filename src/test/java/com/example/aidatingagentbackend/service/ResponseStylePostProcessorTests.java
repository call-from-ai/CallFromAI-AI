package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.entity.MemoryChannel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseSanitizerTests {
    private final ResponseSanitizer processor = new ResponseSanitizer();

    @Test
    void removesEmojiForCallsAndLimitsChatToOne() {
        String call = processor.sanitize("안녕 😊❤️ 반가워", MemoryChannel.CALL);
        String chat = processor.sanitize("안녕 😊❤️ 반가워", MemoryChannel.CHAT);

        assertThat(call).isEqualTo("안녕 반가워");
        assertThat(chat).contains("😊").doesNotContain("❤");
    }

    @Test
    void usesSpeakableFallbackForEmojiOnlyCallReply() {
        String call = processor.sanitize("😊❤️", MemoryChannel.CALL);

        assertThat(call).isEqualTo("응, 듣고 있어");
    }

    @Test
    void doesNotCutCallReplyInTheMiddleOfAKoreanSentence() {
        String original = "오늘 하루도 정말 고생 많았어 이제 편하게 쉬면서 나랑 이야기하자";
        String call = processor.sanitize(original, MemoryChannel.CALL);

        assertThat(call).isEqualTo(original);
    }

    @Test
    void preservesModelWordingAndLengthWithoutSemanticPostProcessing() {
        String firstSentence = "오늘은 정말 바쁜 하루였지만 그래도 네 생각 덕분에 잘 버텼어.";
        String reply = firstSentence + " 이제는 편하게 쉬면서 오늘 있었던 이야기를 천천히 더 나누고 싶어.";
        String processed = processor.sanitize(reply, MemoryChannel.CHAT);

        assertThat(processed).isEqualTo(reply);
    }

    @Test
    void preservesPersonaExpressionsRegardlessOfRelationshipScores() {
        String reply = "자기야, 진짜 짜증나!! ㅋㅋㅋㅋ 그래도 챙겨 먹어.";
        String processed = processor.sanitize(reply, MemoryChannel.CHAT);

        assertThat(processed).isEqualTo(reply);
    }
}
