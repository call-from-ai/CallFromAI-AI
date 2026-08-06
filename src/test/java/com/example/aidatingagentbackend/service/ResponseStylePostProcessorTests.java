package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.RelationshipStrategy;
import com.example.aidatingagentbackend.entity.MemoryChannel;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseStylePostProcessorTests {
    private final ResponseStylePostProcessor processor = new ResponseStylePostProcessor();

    @Test
    void removesEmojiOnlyForCalls() {
        String call = processor.process("안녕 😊❤️ 반가워", MemoryChannel.CALL,
                RelationshipStrategy.NORMAL, 50, 50, null, RelationshipStage.DATING, null);
        String chat = processor.process("안녕 😊❤️ 반가워", MemoryChannel.CHAT,
                RelationshipStrategy.NORMAL, 50, 50, null, RelationshipStage.DATING, null);

        assertThat(call).isEqualTo("안녕 반가워");
        assertThat(chat).contains("😊", "❤");
    }

    @Test
    void usesSpeakableFallbackForEmojiOnlyCallReply() {
        String call = processor.process("😊❤️", MemoryChannel.CALL,
                RelationshipStrategy.NORMAL, 50, 50, null, RelationshipStage.DATING, null);

        assertThat(call).isEqualTo("응, 듣고 있어");
    }
}
