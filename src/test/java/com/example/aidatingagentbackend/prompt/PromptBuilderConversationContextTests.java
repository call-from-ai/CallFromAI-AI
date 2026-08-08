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
                .contains("Length=AROUND_20_CHARACTERS", "must never exceed 20 characters")
                .contains("TimeZone=Asia/Seoul", "TimePeriod=DAWN (새벽)");
    }

    @Test
    void chatPromptIdentifiesTextChat() {
        assertThat(promptBuilder.builder().channel(MemoryChannel.CHAT).build())
                .contains("asynchronous text chat");
    }

    @Test
    void normalizesOffsetDateTimeToUserTimeZoneBeforeResolvingPeriod() {
        String prompt = promptBuilder.builder()
                .character(character())
                .userTimeZone("Asia/Seoul")
                .localDateTime(OffsetDateTime.parse("2026-08-07T15:00:00Z"))
                .build();

        assertThat(prompt)
                .contains("LocalDateTime=2026-08-08T00:00+09:00")
                .contains("DayOfWeek=SATURDAY", "DayType=WEEKEND")
                .contains("Weekend Character Behavior", "Job=개발자")
                .contains("TimePeriod=DAWN (새벽)");
    }

    @Test
    void limitsEveryChatReplyToThirtyCharactersAndOneEmoji() {
        String shortPrompt = promptBuilder.builder().channel(MemoryChannel.CHAT).userMessage("뭐 해?").build();
        String longPrompt = promptBuilder.builder().channel(MemoryChannel.CHAT)
                .userMessage("오늘 있었던 일을 차근차근 길게 이야기해 줄게. 먼저 아침에는 회의가 있었고 점심 이후에는 새로운 프로젝트를 시작했어. 네 생각도 자세히 듣고 싶어.")
                .build();

        assertThat(shortPrompt).contains("Length=MAX_30_CHARACTERS", "one complete, natural Korean sentence", "Emoji=AT_MOST_ONE");
        assertThat(longPrompt).contains("Length=MAX_30_CHARACTERS", "must never exceed 30 characters");
    }

    private CharacterSnapshot character() {
        return new CharacterSnapshot(10L, "하나", "따뜻함", "짧게", "개발자", null, 90,
                new CharacterTraitSnapshot(5, 5, 6, 7, 5, 2, 4, 6, 7, 8, 1));
    }
}
