package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.entity.MemoryChannel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AIProcessingServiceTests {

    @Test
    void callSkipsQualityEvaluation() {
        assertThat(AIProcessingService.shouldSkipQualityEvaluation(MemoryChannel.CALL)).isTrue();
    }

    @Test
    void chatKeepsQualityEvaluation() {
        assertThat(AIProcessingService.shouldSkipQualityEvaluation(MemoryChannel.CHAT)).isFalse();
    }
}
