package com.example.aidatingagentbackend.prompt;

import com.example.aidatingagentbackend.entity.MemoryChannel;

public final class ConversationPromptSection {
    public void appendChannelAndReplyStyle(StringBuilder prompt, MemoryChannel channel) {
        if (channel == MemoryChannel.CALL) {
            prompt.append("[Conversation Channel]\nThis is an ongoing real-time voice call. Use one complete, naturally speakable Korean utterance. Do not use emoji, markdown, bullets, or stage directions.\n");
            prompt.append("[Reply Style]\nLength=CONCISE_CALL (usually about 15-35 Korean characters) Emoji=NONE; prefer a complete and grammatical utterance.\n\n");
        } else {
            prompt.append("[Conversation Channel]\nThis is an asynchronous text chat in natural Korean messenger style.\n");
            prompt.append("[Reply Style]\nLength=CONCISE_CHAT (usually about 20-50 Korean characters) Emoji=AT_MOST_ONE; write a complete and grammatical sentence.\n\n");
        }
    }
}
