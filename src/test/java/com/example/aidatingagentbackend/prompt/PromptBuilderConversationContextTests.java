package com.example.aidatingagentbackend.prompt;

import com.example.aidatingagentbackend.dto.CharacterSnapshot;
import com.example.aidatingagentbackend.dto.CharacterTraitSnapshot;
import com.example.aidatingagentbackend.entity.MemoryChannel;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderConversationContextTests {
    private final PromptBuilder promptBuilder = new PromptBuilder(
            new TraitInstructionResolver(), new RomanceStylePromptResolver());

    @Test
    void callPromptIncludesNamesCallContextAndDawn() {
        String prompt = promptBuilder.builder()
                .character(character())
                .userName("민준")
                .channel(MemoryChannel.CALL)
                .userTimeZone("Asia/Seoul")
                .localDateTime(OffsetDateTime.parse("2026-08-07T02:15:00+09:00"))
                .userMessage("안녕")
                .build();

        assertThat(prompt)
                .contains("UserName=민준", "CharacterName=하나")
                .contains("ongoing real-time voice call", "Do not use emoji")
                .contains("TimeZone=Asia/Seoul", "TimePeriod=DAWN (새벽)");
    }

    @Test
    void chatPromptIdentifiesTextChat() {
        assertThat(promptBuilder.builder().channel(MemoryChannel.CHAT).build())
                .contains("asynchronous text chat");
    }

    private CharacterSnapshot character() {
        return new CharacterSnapshot(10L, "하나", "따뜻함", "짧게", "개발자", null, 90,
                new CharacterTraitSnapshot(5, 5, 6, 7, 5, 2, 4, 6, 7, 8, 1));
    }
}
