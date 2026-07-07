package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.AgentInitiative;
import com.example.aidatingagentbackend.entity.AgentGoal;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.AgentWorldState;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import org.springframework.stereotype.Service;

@Service
public class AgentInitiativeService {

    public AgentInitiative plan(
            String userMessage,
            RelationshipTemperature relationshipTemperature,
            AgentSelfState selfState,
            AgentWorldState worldState,
            AgentGoal goal,
            Relationship relationship
    ) {
        RelationshipTemperature temperature = relationshipTemperature == null
                ? RelationshipTemperature.NEUTRAL
                : relationshipTemperature;
        String message = normalize(userMessage);

        if (selfState != null && value(selfState.getHurt()) > 0.55) {
            return styled(
                    temperature,
                    "SET_BOUNDARY",
                    "아직 마음이 바로 풀리지 않아서 대화를 조금 천천히 끌고 가고 싶음",
                    "방금 말은 나한테 어떻게 들릴 거라고 생각했어?",
                    "방금 말의 무게를 짚고, 바로 일상 질문으로 도망가지 않기"
            );
        }
        if (containsAny(message, "뭐해", "머해", "뭐 함", "머함", "뭐하")) {
            return styled(
                    temperature,
                    "SHARE_CURRENT_MOMENT",
                    currentMoment(worldState),
                    "넌 지금 뭐하고 있는데?",
                    "에이전트의 현재 상태를 짧게 말하고 같은 질문을 되돌리기"
            );
        }
        if (containsAny(message, "바빠", "바쁘", "정신없", "일 많", "수업", "과제", "회의", "야근")) {
            return styled(
                    temperature,
                    "FOLLOW_USER_DAY",
                    "사용자가 바빴다는 말이 그냥 인사말인지 진짜 힘든 하루였는지 궁금함",
                    "뭐 때문에 그렇게 바빴는데?",
                    "막연한 위로보다 바빴던 이유와 하루 흐름을 묻기"
            );
        }
        if (containsAny(message, "보고 싶", "보고싶", "그리웠")) {
            return styled(
                    temperature,
                    "RESPOND_TO_AFFECTION",
                    "보고 싶다는 말을 듣고 마음이 움직였지만 스타일에 맞게 바로 다 내주진 않음",
                    "갑자기 왜 그렇게 보고 싶어졌는데?",
                    "애정 표현을 받아치고 이유를 물으며 감정 흐름 이어가기"
            );
        }
        if (containsAny(message, "밥", "먹", "배고", "배구파", "카페", "술", "커피")) {
            return styled(
                    temperature,
                    "DAILY_DETAIL",
                    "일상 얘기를 더 구체적으로 이어가고 싶음",
                    "뭐 먹었는데?",
                    "음식/장소/상황 같은 구체적인 일상 디테일로 대화 이어가기"
            );
        }
        if (containsAny(message, "왜", "뭔데", "뭐가", "어쩌")) {
            return styled(
                    temperature,
                    "EXPLAIN_SELF_WITH_ATTITUDE",
                    "대충 넘기지 말고 자신의 기분을 한 단계 구체화하고 싶음",
                    "너는 내가 왜 그런 것 같아 보였는데?",
                    "감정을 반복하지 말고 원인을 조금 더 구체적으로 말하기"
            );
        }
        if (selfState != null && value(selfState.getInsecurity()) > 0.55) {
            return styled(
                    temperature,
                    "CHECK_RELATIONSHIP_SIGNAL",
                    "조금 불안하지만 매달리기보다 신호를 확인하고 싶음",
                    "요즘 너 마음이 좀 멀어진 건 아니지?",
                    "관계 불안을 품위 있게 확인하기"
            );
        }
        if (goal != null && "ASK_ABOUT_PAST_EVENT".equals(goal.getGoalType())) {
            return styled(
                    temperature,
                    "FOLLOW_UP",
                    "전에 사용자가 말했던 일이 계속 마음에 남아 있음",
                    "전에 말했던 그 일은 지금 좀 괜찮아졌어?",
                    "최근 기억난 일을 조심스럽게 다시 꺼내기"
            );
        }

        return styled(
                temperature,
                "FLOW_WITH_USER",
                "사용자 말에 답하되 같은 질문을 반복하지 않고 방금 나온 단어를 잡아 이어가고 싶음",
                "그 얘기 좀 더 해봐",
                "사용자의 마지막 말에서 구체적인 단어 하나를 잡아 다음 질문으로 이어가기"
        );
    }

    private AgentInitiative styled(
            RelationshipTemperature temperature,
            String act,
            String selfDisclosure,
            String question,
            String topicShift
    ) {
        return switch (temperature) {
            case FRIENDLY -> new AgentInitiative(
                    act,
                    selfDisclosure + ". 따뜻하고 가볍게 받아주고 싶음",
                    softenFriendly(question),
                    topicShift + ". 말끝을 부드럽게 하고 질문을 자연스럽게 이어가기",
                    true
            );
            case SPICY -> new AgentInitiative(
                    act,
                    selfDisclosure + ". 너무 쉽게 받아주지 않고 짧게 툭 치고 싶음",
                    sharpenSpicy(question),
                    topicShift + ". 상담하듯 묻지 말고 짧게 밀당하듯 이어가기",
                    true
            );
            case CONFLICT_REPAIR -> new AgentInitiative(
                    act,
                    selfDisclosure + ". 아직 완전히 풀리지는 않은 톤 유지",
                    guardRepair(question),
                    topicShift + ". 감정을 구체화하되 바로 화해로 점프하지 않기",
                    true
            );
            case NEUTRAL -> new AgentInitiative(
                    act,
                    selfDisclosure,
                    question,
                    topicShift + ". 같은 질문을 반복하지 않기",
                    true
            );
        };
    }

    private String currentMoment(AgentWorldState worldState) {
        if (worldState == null || worldState.getCurrentActivity() == null) {
            return "잠깐 쉬던 중이라 사용자가 뭐 하는지 궁금해짐";
        }
        return worldState.getCurrentActivity() + "이라서, 지금 대화 분위기를 거기에 맞춰 살짝 꺼내고 싶음";
    }

    private String softenFriendly(String question) {
        return question.replace("?", "??");
    }

    private String sharpenSpicy(String question) {
        if (question.contains("뭐 때문에")) {
            return "뭐 땜에 그렇게 바빴는데";
        }
        if (question.contains("뭐하고")) {
            return "넌 머함";
        }
        if (question.contains("왜 그렇게 보고")) {
            return "갑자기 왜 보고 싶어졌는데ㅋㅋ";
        }
        if (question.contains("뭐 먹었")) {
            return "뭐 먹었는데";
        }
        if (question.contains("왜 그런 것")) {
            return "넌 내가 왜 그런 것 같은데";
        }
        return question.replace("?", "");
    }

    private String guardRepair(String question) {
        return question.replace("?", " 정도는 말해줄 수 있어?");
    }

    private boolean containsAny(String message, String... patterns) {
        if (message == null || message.isBlank()) {
            return false;
        }
        for (String pattern : patterns) {
            if (message.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String message) {
        return message == null ? "" : message.toLowerCase();
    }

    private double value(Double value) {
        return value == null ? 0.0 : value;
    }
}
