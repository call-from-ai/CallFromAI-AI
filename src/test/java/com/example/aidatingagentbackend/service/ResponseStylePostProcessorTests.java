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
    void limitsCallReplyToTwentyCharacters() {
        String call = processor.process("오늘 하루도 정말 고생 많았어 이제 편하게 쉬면서 나랑 이야기하자",
                MemoryChannel.CALL, RelationshipStrategy.NORMAL, 50, 50, null,
                RelationshipStage.DATING, null);

        assertThat(call.length()).isLessThanOrEqualTo(20);
    }

    @Test
    void limitsChatReplyToThirtyCharacters() {
        String reply = "가".repeat(150);
        String processed = processor.process(reply, MemoryChannel.CHAT,
                RelationshipStrategy.NORMAL, 50, 50, null, RelationshipStage.DATING, null, "응");

        assertThat(processed).hasSize(30);
    }
}
