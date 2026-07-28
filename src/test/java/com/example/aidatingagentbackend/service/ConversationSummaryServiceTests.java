package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.ConversationSummaryRequest;
import com.example.aidatingagentbackend.dto.ConversationSummaryResponse;
import com.example.aidatingagentbackend.dto.SummaryMessage;
import com.example.aidatingagentbackend.exception.GeminiCallException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationSummaryServiceTests {

    @Mock
    private GeminiService geminiService;

    private ConversationSummaryService service;

    @BeforeEach
    void setUp() {
        service = new ConversationSummaryService(geminiService);
    }

    @Test
    void includesPreviousSummaryAndMessagesInPrompt() {
        when(geminiService.generate(anyString())).thenReturn(" 아이스티를 좋아해요. ");

        ConversationSummaryResponse response = service.summarize(request(200));

        assertEquals("아이스티를 좋아해요.", response.summary());
        verify(geminiService).generate(org.mockito.ArgumentMatchers.argThat(prompt ->
                prompt.contains("기존 취향") &&
                        prompt.contains("민준의 관심사, 취향, 성격") &&
                        prompt.contains("일반 호칭을 절대 사용하지 말 것") &&
                        prompt.contains("사용자는 반드시 '민준'") &&
                        prompt.contains("AI 캐릭터는 반드시 '하나'") &&
                        prompt.contains("참여자 이름 자체를 제외하고 영어 단어나 로마자 표현을 사용하지 말 것") &&
                        prompt.indexOf("민준: 첫 메시지") < prompt.indexOf("하나: 두 번째 메시지") &&
                        !prompt.contains("user: 첫 메시지") &&
                        !prompt.contains("assistant: 두 번째 메시지")));
    }

    @Test
    void replacesGenericParticipantLabelsButKeepsAiUsedAsContent() {
        when(geminiService.generate(anyString())).thenReturn(
                "AI는 사용자에게 연락했고, 사용자는 AI 과제를 궁금해했습니다.");

        ConversationSummaryResponse response = service.summarize(request(200));

        assertEquals("하나는 민준에게 연락했고, 민준은 AI 과제를 궁금해했습니다.", response.summary());
    }

    @Test
    void normalizesJsonEscapedDoubleQuotesToKoreanQuotationMarks() {
        when(geminiService.generate(anyString())).thenReturn(
                "민준은 \\\"대충\\\"이라며 상황을 넘겼습니다.");

        ConversationSummaryResponse response = service.summarize(request(200));

        assertEquals("민준은 ‘대충’이라며 상황을 넘겼습니다.", response.summary());
    }

    @Test
    void neverReturnsMoreThanTwoHundredUnicodeCharacters() {
        when(geminiService.generate(anyString())).thenReturn("가".repeat(199) + "😀😀");

        ConversationSummaryResponse response = service.summarize(request(500));

        assertEquals(200, response.summary().codePointCount(0, response.summary().length()));
        assertTrue(response.summary().endsWith("😀"));
    }

    @Test
    void rejectsBlankGeminiResult() {
        when(geminiService.generate(anyString())).thenReturn("  ");

        assertThrows(GeminiCallException.class, () -> service.summarize(request(200)));
    }

    @Test
    void rejectsUnsupportedRole() {
        ConversationSummaryRequest invalid = new ConversationSummaryRequest(
                1L, "민준", "하나", null, List.of(new SummaryMessage("system", "내용")), 200);

        assertThrows(IllegalArgumentException.class, () -> service.summarize(invalid));
    }

    @Test
    void rejectsBlankParticipantName() {
        ConversationSummaryRequest invalid = new ConversationSummaryRequest(
                1L, " ", "하나", null, List.of(new SummaryMessage("user", "내용")), 200);

        assertThrows(IllegalArgumentException.class, () -> service.summarize(invalid));
    }

    private ConversationSummaryRequest request(int maxCharacters) {
        return new ConversationSummaryRequest(
                1L,
                "민준",
                "하나",
                "기존 취향",
                List.of(
                        new SummaryMessage("user", "첫 메시지"),
                        new SummaryMessage("assistant", "두 번째 메시지")
                ),
                maxCharacters
        );
    }
}
