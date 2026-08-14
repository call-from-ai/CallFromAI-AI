package com.example.aidatingagentbackend.prompt;

import com.example.aidatingagentbackend.entity.MemoryChannel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptContextSelectorTests {
    private final PromptContextSelector selector = new PromptContextSelector();

    @Test
    void ordinaryConcernOmitsUnrelatedPastAndScheduleContext() {
        var selection = selector.select("회사에서 속상한 일이 있었어", MemoryChannel.CHAT, true);
        assertThat(selection.timeDetail()).isFalse();
        assertThat(selection.memory()).isFalse();
        assertThat(selection.sharedEvents()).isFalse();
        assertThat(selection.styleExamples()).isFalse();
    }

    @Test
    void explicitPastQuestionEnablesMemoryAndSharedEvents() {
        var selection = selector.select("우리 전에 했던 말 기억해?", MemoryChannel.CHAT, true);
        assertThat(selection.memory()).isTrue();
        assertThat(selection.sharedEvents()).isTrue();
    }
}
