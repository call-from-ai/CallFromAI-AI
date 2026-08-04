package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.CallTopicRequest;
import com.example.aidatingagentbackend.dto.CallTopicResponse;
import com.example.aidatingagentbackend.dto.SummaryMessage;
import com.example.aidatingagentbackend.entity.MemoryChannel;
import com.example.aidatingagentbackend.exception.GeminiCallException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
public class CallTopicService {

    static final int MAX_CHARACTERS = 20;
    static final int MAX_OUTPUT_TOKENS = 40;
    private static final Set<String> ALLOWED_ROLES = Set.of("user", "assistant");

    private final GeminiService geminiService;

    public CallTopicService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    public CallTopicResponse createTopic(CallTopicRequest request) {
        validate(request);
        int limit = Math.min(request.maxCharacters(), MAX_CHARACTERS);
        String topic = geminiService.generate(
                buildPrompt(request.messages(), limit),
                MemoryChannel.CALL,
                MAX_OUTPUT_TOKENS
        ).strip();

        if (!StringUtils.hasText(topic)) {
            throw new GeminiCallException("Gemini returned an empty call topic.", null);
        }
        return new CallTopicResponse(topic);
    }

    String buildPrompt(List<SummaryMessage> messages, int limit) {
        StringBuilder prompt = new StringBuilder("""
                다음 통화 한 건에서 무엇을 이야기했는지 나타내는 한국어 주제 라벨 하나를 작성하라.

                규칙:
                - 공백을 포함해 반드시 %d자 이내로 완결할 것
                - 넘는 문장을 만든 뒤 자르지 말고, 처음부터 제한 안에 들어오는 짧은 표현을 만들 것
                - 화자 이름이나 사람 이름을 쓰지 말 것
                - '사용자', '유저', 'AI', 'AI 캐릭터', '캐릭터', 'assistant' 등 화자 호칭을 쓰지 말 것
                - 누가 말했는지가 아니라 무엇을 이야기했는지만 표현할 것
                - 문장 종결어미 없이 명사형 한 줄로 작성할 것
                - 줄바꿈을 절대 사용하지 말고 반드시 한 줄로만 출력할 것
                - 대화에 없는 내용을 추측하지 말 것
                - 따옴표, 마침표, 접두 설명 없이 라벨만 출력할 것

                통화 내용(시간순):
                """.formatted(limit));

        for (SummaryMessage message : messages) {
            prompt.append(message.role()).append(": ").append(message.content().strip()).append('\n');
        }
        return prompt.toString();
    }

    private void validate(CallTopicRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        if (request.callId() == null || request.callId() <= 0) {
            throw new IllegalArgumentException("callId must be a positive number.");
        }
        if (request.maxCharacters() == null || request.maxCharacters() <= 0) {
            throw new IllegalArgumentException("maxCharacters must be a positive number.");
        }
        if (request.messages() == null || request.messages().isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty.");
        }
        for (SummaryMessage message : request.messages()) {
            if (message == null || !ALLOWED_ROLES.contains(message.role())) {
                throw new IllegalArgumentException("message role must be user or assistant.");
            }
            if (!StringUtils.hasText(message.content())) {
                throw new IllegalArgumentException("message content must not be blank.");
            }
        }
    }
}
