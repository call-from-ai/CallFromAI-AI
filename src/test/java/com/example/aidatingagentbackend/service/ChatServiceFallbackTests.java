package com.example.aidatingagentbackend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.aidatingagentbackend.dto.ChatRequest;
import com.example.aidatingagentbackend.entity.MemoryChannel;
import com.example.aidatingagentbackend.exception.GeminiCallException;
import com.example.aidatingagentbackend.exception.GeminiTimeoutException;
import org.junit.jupiter.api.Test;

class ChatServiceFallbackTests {

    @Test
    void callGeminiFailureBeforeFirstChunkUsesPreparedFallback() {
        ChatRequest request = request(MemoryChannel.CALL);

        assertThat(ChatService.shouldSendCallFallback(
                request, new GeminiCallException("failed", null), false)).isTrue();
        assertThat(ChatService.shouldSendCallFallback(
                request, new GeminiTimeoutException("timeout", null), false)).isTrue();
        assertThat(ChatService.callFailureFallback()).isNotBlank();
    }

    @Test
    void chatChannelKeepsErrorEventBehavior() {
        assertThat(ChatService.shouldSendCallFallback(
                request(MemoryChannel.CHAT), new GeminiCallException("failed", null), false)).isFalse();
    }

    @Test
    void partialCallDoesNotAppendFallbackAfterSpokenText() {
        assertThat(ChatService.shouldSendCallFallback(
                request(MemoryChannel.CALL), new GeminiCallException("failed", null), true)).isFalse();
    }

    @Test
    void nonGeminiFailureIsNotHiddenAsFallback() {
        assertThat(ChatService.shouldSendCallFallback(
                request(MemoryChannel.CALL), new IllegalStateException("internal"), false)).isFalse();
    }

    private static ChatRequest request(MemoryChannel channel) {
        ChatRequest request = new ChatRequest();
        request.setChannel(channel);
        return request;
    }
}
