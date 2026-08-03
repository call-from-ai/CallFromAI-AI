package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.CallTopicRequest;
import com.example.aidatingagentbackend.dto.CallTopicResponse;
import com.example.aidatingagentbackend.dto.SummaryMessage;
import com.example.aidatingagentbackend.entity.MemoryChannel;
import com.example.aidatingagentbackend.exception.GeminiCallException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallTopicServiceTests {

    @Mock
    private GeminiService geminiService;

    private CallTopicService service;

    @BeforeEach
    void setUp() {
        service = new CallTopicService(geminiService);
    }

    @Test
    void createsParticipantNeutralTopicWithFastGenerationOptions() {
        when(geminiService.generate(anyString(), org.mockito.ArgumentMatchers.eq(MemoryChannel.CALL),
                org.mockito.ArgumentMatchers.eq(40)))
                .thenReturn(" 오늘하루와 퇴근 후 일상 이야기 ");

        CallTopicResponse response = service.createTopic(request(20));

        assertEquals("오늘하루와 퇴근 후 일상 이야기", response.topic());
        verify(geminiService).generate(org.mockito.ArgumentMatchers.argThat(prompt ->
                        prompt.contains("반드시 20자 이내")
                                && prompt.contains("사람 이름을 쓰지 말 것")
                                && prompt.indexOf("user: 오늘 퇴근하고 뭐 했어?")
                                < prompt.indexOf("assistant: 집에서 좀 쉬었어. 너는?")),
                org.mockito.ArgumentMatchers.eq(MemoryChannel.CALL),
                org.mockito.ArgumentMatchers.eq(40));
    }

    @Test
    void rejectsOverLimitResultInsteadOfTruncatingIt() {
        when(geminiService.generate(anyString(), org.mockito.ArgumentMatchers.eq(MemoryChannel.CALL),
                org.mockito.ArgumentMatchers.eq(40))).thenReturn("가".repeat(21));

        assertThrows(GeminiCallException.class, () -> service.createTopic(request(20)));
    }

    @Test
    void usesRequestedLimitWhenItIsBelowTwenty() {
        when(geminiService.generate(anyString(), org.mockito.ArgumentMatchers.eq(MemoryChannel.CALL),
                org.mockito.ArgumentMatchers.eq(40))).thenReturn("짧은 주제");

        service.createTopic(request(10));

        verify(geminiService).generate(org.mockito.ArgumentMatchers.argThat(prompt ->
                        prompt.contains("반드시 10자 이내")),
                org.mockito.ArgumentMatchers.eq(MemoryChannel.CALL),
                org.mockito.ArgumentMatchers.eq(40));
    }

    @Test
    void rejectsUnsupportedRole() {
        CallTopicRequest invalid = new CallTopicRequest(
                123L, List.of(new SummaryMessage("system", "내용")), 20);

        assertThrows(IllegalArgumentException.class, () -> service.createTopic(invalid));
    }

    private CallTopicRequest request(int maxCharacters) {
        return new CallTopicRequest(
                123L,
                List.of(
                        new SummaryMessage("user", "오늘 퇴근하고 뭐 했어?"),
                        new SummaryMessage("assistant", "집에서 좀 쉬었어. 너는?")
                ),
                maxCharacters
        );
    }
}
