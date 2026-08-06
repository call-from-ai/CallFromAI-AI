package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.ConversationSummaryRequest;
import com.example.aidatingagentbackend.dto.ConversationSummaryResponse;
import com.example.aidatingagentbackend.dto.SummaryMessage;
import com.example.aidatingagentbackend.exception.GeminiCallException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ConversationSummaryService {

    static final int MAX_CHARACTERS = 15;
    private static final Set<String> ALLOWED_ROLES = Set.of("user", "assistant");
    private static final String PARTICIPANT_PARTICLES = "에게|은|는|이|가|을|를|의|와|과|도|만";

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

        String namedSummary = replaceGenericParticipantLabels(
                generatedSummary,
                request.userName().strip(),
                request.characterName().strip()
        );
        String normalizedSummary = normalizeQuotationMarks(namedSummary);
        return new ConversationSummaryResponse(truncateByCodePoint(normalizedSummary, limit));
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
                - 인물을 지칭할 때 '사용자', '유저', 'AI', 'AI 캐릭터', '캐릭터', 'assistant'라는 일반 호칭을 절대 사용하지 말 것
                - 사용자는 반드시 '%s', AI 캐릭터는 반드시 '%s'라는 이름으로 지칭할 것
                - 기존 요약에 일반 호칭이 있더라도 새 요약에서는 반드시 위 이름으로 바꿀 것
                - 참여자 이름 자체를 제외하고 영어 단어나 로마자 표현을 사용하지 말 것
                - 영어 부사나 감정 표현도 반드시 자연스러운 한국어로 번역할 것
                - 큰따옴표(")를 사용하지 말고 필요한 경우 한국어 인용부호(‘ ’)를 사용할 것
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

    private String replaceGenericParticipantLabels(String summary, String userName, String characterName) {
        String result = replaceParticipantLabel(summary, "사용자", userName);
        result = replaceParticipantLabel(result, "유저", userName);
        result = replaceParticipantLabel(result, "AI 캐릭터", characterName);
        result = replaceParticipantLabel(result, "캐릭터", characterName);
        result = replaceParticipantLabel(result, "assistant", characterName);
        return replaceParticipantLabel(result, "AI", characterName);
    }

    private String normalizeQuotationMarks(String value) {
        String unescaped = value.replace("\\\"", "\"");
        StringBuilder normalized = new StringBuilder(unescaped.length());
        boolean opening = true;
        for (int index = 0; index < unescaped.length(); index++) {
            char current = unescaped.charAt(index);
            if (current == '"') {
                normalized.append(opening ? '‘' : '’');
                opening = !opening;
            } else {
                normalized.append(current);
            }
        }
        return normalized.toString();
    }

    private String replaceParticipantLabel(String value, String genericLabel, String name) {
        Pattern participantUse = Pattern.compile(
                Pattern.quote(genericLabel) + "(" + PARTICIPANT_PARTICLES + ")"
        );
        Matcher matcher = participantUse.matcher(value);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String replacement = name + adjustParticle(matcher.group(1), name);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String adjustParticle(String particle, String name) {
        boolean hasFinalConsonant = hasKoreanFinalConsonant(name);
        return switch (particle) {
            case "은", "는" -> hasFinalConsonant ? "은" : "는";
            case "이", "가" -> hasFinalConsonant ? "이" : "가";
            case "을", "를" -> hasFinalConsonant ? "을" : "를";
            case "과", "와" -> hasFinalConsonant ? "과" : "와";
            default -> particle;
        };
    }

    private boolean hasKoreanFinalConsonant(String name) {
        int lastCodePoint = name.codePointBefore(name.length());
        return lastCodePoint >= 0xAC00
                && lastCodePoint <= 0xD7A3
                && (lastCodePoint - 0xAC00) % 28 != 0;
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
