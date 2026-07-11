package com.example.aidatingagentbackend.engine;

import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
public class MessageSignalDetector {

    private static final List<String> BREAKUP_DECLARATION = List.of(
            "헤어지자", "우리 끝내자", "끝내자", "그만 만나자", "그만 만나", "이별하자"
    );
    private static final List<String> BREAKUP_RETRACTION = List.of(
            "아니야 농담", "농담이야", "방금 말 취소", "취소할게", "아니야 괜찮아", "아냐 괜찮아"
    );
    private static final List<String> APOLOGY = List.of(
            "미안", "죄송", "내가 잘못", "잘못했어", "사과", "사과할게"
    );
    private static final List<String> AFFECTION = List.of(
            "사랑해", "좋아해", "보고 싶", "보고싶", "네 생각났어", "그리웠"
    );
    private static final List<String> INSULT = List.of(
            "짜증나", "한심", "최악", "싫어", "질려", "귀찮아"
    );
    private static final List<String> IGNORE_OR_COLD = List.of(
            "몰라", "상관없어", "됐어", "말 걸지마", "답하기 싫어", "귀찮으니까"
    );
    private static final List<String> AMBIGUOUS_IMPORTANT = List.of(
            "미래가 안 보여", "생각할 시간", "거리 두자", "힘들어", "상처", "서운", "실망", "무서워", "불안"
    );
    private static final List<String> USER_RETURNED_TO_TALK = List.of(
            "너랑 얘기", "너랑 말", "얘기하려고", "말하려고", "보려고 왔", "왔다", "왔어"
    );
    private static final List<String> ASK_AGENT_SELF_DISCLOSURE = List.of(
            "너 얘기", "네 얘기", "니 얘기", "어제 뭐", "어제 머", "어제 뭐했", "뭐 했어", "머 했어",
            "뭐했어", "머했어", "뭐하고 있었", "뭐했는데", "너는", "넌"
    );
    private static final List<String> USER_SKIPPED_MEAL = List.of(
            "안 먹", "못 먹", "굶", "저녁 안", "밥 안"
    );
    private static final List<String> CLUB = List.of("동아리");
    private static final List<String> DEVELOPMENT = List.of(
            "개발", "코딩", "앱", "백엔드", "프론트", "프로젝트"
    );
    private static final List<String> ASSIGNMENT_OR_CLASS = List.of(
            "과제", "시험", "수업"
    );
    private static final List<String> WORK_OR_BUSY = List.of(
            "바빠", "바쁘", "정신없", "일 많", "회의", "야근"
    );

    public MessageSignals detect(String userMessage) {
        String normalized = normalize(userMessage);
        Set<MessageSignalType> types = EnumSet.noneOf(MessageSignalType.class);

        addIfContains(types, normalized, MessageSignalType.BREAKUP_DECLARATION, BREAKUP_DECLARATION);
        addIfContains(types, normalized, MessageSignalType.BREAKUP_RETRACTION, BREAKUP_RETRACTION);
        addIfContains(types, normalized, MessageSignalType.APOLOGY, APOLOGY);
        addIfContains(types, normalized, MessageSignalType.AFFECTION, AFFECTION);
        addIfContains(types, normalized, MessageSignalType.INSULT, INSULT);
        addIfContains(types, normalized, MessageSignalType.IGNORE_OR_COLD, IGNORE_OR_COLD);
        addIfContains(types, normalized, MessageSignalType.AMBIGUOUS_IMPORTANT, AMBIGUOUS_IMPORTANT);
        addIfContains(types, normalized, MessageSignalType.USER_RETURNED_TO_TALK, USER_RETURNED_TO_TALK);
        addIfContains(types, normalized, MessageSignalType.ASK_AGENT_SELF_DISCLOSURE, ASK_AGENT_SELF_DISCLOSURE);
        addIfContains(types, normalized, MessageSignalType.USER_SKIPPED_MEAL, USER_SKIPPED_MEAL);
        addIfContains(types, normalized, MessageSignalType.CLUB, CLUB);
        addIfContains(types, normalized, MessageSignalType.DEVELOPMENT, DEVELOPMENT);
        addIfContains(types, normalized, MessageSignalType.ASSIGNMENT_OR_CLASS, ASSIGNMENT_OR_CLASS);
        addIfContains(types, normalized, MessageSignalType.WORK_OR_BUSY, WORK_OR_BUSY);

        return new MessageSignals(normalized, types);
    }

    private void addIfContains(
            Set<MessageSignalType> types,
            String message,
            MessageSignalType type,
            List<String> keywords
    ) {
        if (containsAny(message, keywords)) {
            types.add(type);
        }
    }

    private boolean containsAny(String message, List<String> keywords) {
        if (message == null || message.isBlank()) {
            return false;
        }
        return keywords.stream()
                .map(String::toLowerCase)
                .anyMatch(message::contains);
    }

    private String normalize(String message) {
        return message == null ? "" : message.toLowerCase();
    }
}
