package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.ConversationSummaryRequest;
import com.example.aidatingagentbackend.dto.ConversationSummaryResponse;
import com.example.aidatingagentbackend.dto.SummaryMessage;
import com.example.aidatingagentbackend.exception.GeminiCallException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
public class ConversationSummaryService {

    static final int MAX_CHARACTERS = 200;
    private static final Set<String> ALLOWED_ROLES = Set.of("user", "assistant");

    private final GeminiService geminiService;

    public ConversationSummaryService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    public ConversationSummaryResponse summarize(ConversationSummaryRequest request) {
        validate(request);

        int limit = Math.min(request.maxCharacters(), MAX_CHARACTERS);
        String generatedSummary = geminiService.generate(buildPrompt(request, limit)).strip();
        if (!StringUtils.hasText(generatedSummary)) {
            throw new GeminiCallException("Gemini returned an empty conversation summary.", null);
        }

        return new ConversationSummaryResponse(truncateByCodePoint(generatedSummary, limit));
    }

    String buildPrompt(ConversationSummaryRequest request, int limit) {
        String userName = request.userName().strip();
        String characterName = request.characterName().strip();
        StringBuilder prompt = new StringBuilder("""
                %s와 %s가 나눈 대화를 요약하라.

                규칙:
                - 공백 포함 %d자 이내
                - %s의 관심사, 취향, 성격을 중심으로 작성
                - %s와 %s의 관계나 대화 분위기도 포함
                - 대화에 없는 내용을 추측하지 말 것
                - 비밀번호, 연락처 등 민감정보는 제외
                - 자연스러운 한국어 문장으로 작성
                - 요약문만 출력

                관계 ID: %d
                참여자:
                - 사용자: %s
                - 캐릭터: %s

                기존 요약:
                %s

                새 대화(아래 순서가 시간순):
                """.formatted(
                userName,
                characterName,
                limit,
                userName,
                userName,
                characterName,
                request.relationshipId(),
                userName,
                characterName,
                StringUtils.hasText(request.previousSummary()) ? request.previousSummary().strip() : "(없음)"
        ));

        for (SummaryMessage message : request.messages()) {
            prompt.append("user".equals(message.role()) ? userName : characterName)
                    .append(": ")
                    .append(message.content().strip())
                    .append('\n');
        }
        return prompt.toString();
    }

    private void validate(ConversationSummaryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        if (request.relationshipId() == null || request.relationshipId() <= 0) {
            throw new IllegalArgumentException("relationshipId must be a positive number.");
        }
        validateName(request.userName(), "userName");
        validateName(request.characterName(), "characterName");
        if (request.maxCharacters() == null || request.maxCharacters() <= 0) {
            throw new IllegalArgumentException("maxCharacters must be a positive number.");
        }
        List<SummaryMessage> messages = request.messages();
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty.");
        }
        for (SummaryMessage message : messages) {
            if (message == null || !ALLOWED_ROLES.contains(message.role())) {
                throw new IllegalArgumentException("message role must be user or assistant.");
            }
            if (!StringUtils.hasText(message.content())) {
                throw new IllegalArgumentException("message content must not be blank.");
            }
        }
    }

    private void validateName(String name, String fieldName) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        if (name.contains("\n") || name.contains("\r")) {
            throw new IllegalArgumentException(fieldName + " must not contain line breaks.");
        }
    }

    private String truncateByCodePoint(String value, int maxCodePoints) {
        int codePointCount = value.codePointCount(0, value.length());
        if (codePointCount <= maxCodePoints) {
            return value;
        }
        int endIndex = value.offsetByCodePoints(0, maxCodePoints);
        return value.substring(0, endIndex).stripTrailing();
    }
}
