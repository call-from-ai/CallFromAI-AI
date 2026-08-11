package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.RelationshipStrategy;
import com.example.aidatingagentbackend.entity.MemoryChannel;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseStylePostProcessorTests {
    private final ResponseStylePostProcessor processor = new ResponseStylePostProcessor();

    @Test
    void removesEmojiForCallsAndLimitsChatToOne() {
        String call = processor.process("안녕 😊❤️ 반가워", MemoryChannel.CALL,
                RelationshipStrategy.NORMAL, 50, 50, null, RelationshipStage.DATING, null);
        String chat = processor.process("안녕 😊❤️ 반가워", MemoryChannel.CHAT,
                RelationshipStrategy.NORMAL, 50, 50, null, RelationshipStage.DATING, null);

        assertThat(call).isEqualTo("안녕 반가워");
        assertThat(chat).contains("😊").doesNotContain("❤");
    }

    @Test
    void usesSpeakableFallbackForEmojiOnlyCallReply() {
        String call = processor.process("😊❤️", MemoryChannel.CALL,
                RelationshipStrategy.NORMAL, 50, 50, null, RelationshipStage.DATING, null);

        assertThat(call).isEqualTo("응, 듣고 있어");
    }

    @Test
    void doesNotCutCallReplyInTheMiddleOfAKoreanSentence() {
        String original = "오늘 하루도 정말 고생 많았어 이제 편하게 쉬면서 나랑 이야기하자";
        String call = processor.process(original,
                MemoryChannel.CALL, RelationshipStrategy.NORMAL, 50, 50, null,
                RelationshipStage.DATING, null);

        assertThat(call).isEqualTo(original);
    }

    @Test
    void trimsLongChatOnlyAtACompleteSentenceBoundary() {
        String firstSentence = "오늘은 정말 바쁜 하루였지만 그래도 네 생각 덕분에 잘 버텼어.";
        String reply = firstSentence + " 이제는 편하게 쉬면서 오늘 있었던 이야기를 천천히 더 나누고 싶어.";
        String processed = processor.process(reply, MemoryChannel.CHAT,
                RelationshipStrategy.NORMAL, 50, 50, null, RelationshipStage.DATING, null, "응");

        assertThat(processed).isEqualTo(firstSentence);
    }
}
