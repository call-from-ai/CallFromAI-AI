package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.AgentInitiative;
import com.example.aidatingagentbackend.dto.RelationshipStrategy;
import com.example.aidatingagentbackend.entity.AgentGoal;
import com.example.aidatingagentbackend.entity.AgentLifeEvent;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.AgentWorldState;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class AgentInitiativeService {

    public AgentInitiative plan(
            String userMessage,
            RelationshipStrategy relationshipStrategy,
            AgentSelfState selfState,
            AgentWorldState worldState,
            AgentGoal goal,
            List<AgentLifeEvent> lifeEvents
    ) {
        RelationshipStrategy strategy = relationshipStrategy == null ? RelationshipStrategy.NORMAL : relationshipStrategy;
        String message = normalize(userMessage);

        if (asksAboutAgentLife(message)) {
            AgentLifeEvent event = pickLifeEvent(message, lifeEvents);
            return styled(
                    strategy,
                    "ANSWER_ABOUT_AGENT_LIFE",
                    lifeDisclosure(event),
                    "너는 어제 뭐했는데?",
                    "회피하지 말고 에이전트의 어제/오늘 일을 최소 한 가지 구체적으로 말한 뒤 되묻기"
            );
        }
        if (selfState != null && value(selfState.getHurt()) > 0.55) {
            return styled(
                    strategy,
                    "SET_BOUNDARY",
                    "아직 마음이 바로 풀리지 않아서 대화를 조금 천천히 끌고 가고 싶음",
                    "방금 말은 나한테 어떻게 들릴 거라고 생각했어?",
                    "방금 말의 무게를 짚고, 바로 일상 질문으로 도망가지 않기"
            );
        }
        if (containsAny(message, "뭐해", "머해", "뭐 함", "머함", "뭐하")) {
            return styled(
                    strategy,
                    "SHARE_CURRENT_MOMENT",
                    currentMoment(worldState),
                    "넌 지금 뭐하고 있는데?",
                    "에이전트의 현재 상태를 짧게 말하고 같은 질문을 되돌리기"
            );
        }
        if (containsAny(message, "바빠", "바쁘", "정신없", "일 많", "수업", "과제", "회의", "야근")) {
            return styled(
                    strategy,
                    "FOLLOW_USER_DAY",
                    "사용자가 바빴다는 말이 그냥 인사말인지 진짜 힘든 하루였는지 궁금함",
                    "뭐 때문에 그렇게 바빴는데?",
                    "막연한 위로보다 바빴던 이유와 하루 흐름을 묻기"
            );
        }
        if (containsAny(message, "보고 싶", "보고싶", "그리웠")) {
            return styled(
                    strategy,
                    "RESPOND_TO_AFFECTION",
                    "보고 싶다는 말을 듣고 마음이 움직였지만 스타일에 맞게 바로 다 내주진 않음",
                    "갑자기 왜 그렇게 보고 싶어졌는데?",
                    "애정 표현을 받아치고 이유를 물으며 감정 흐름 이어가기"
            );
        }
        if (containsAny(message, "밥", "먹", "배고", "배구파", "카페", "술", "커피")) {
            return styled(
                    strategy,
                    "DAILY_DETAIL",
                    "일상 얘기를 더 구체적으로 이어가고 싶음",
                    "뭐 먹었는데?",
                    "음식/장소/상황 같은 구체적인 일상 디테일로 대화 이어가기"
            );
        }
        if (containsAny(message, "왜", "뭔데", "뭐가", "어쩌")) {
            return styled(
                    strategy,
                    "EXPLAIN_SELF_WITH_ATTITUDE",
                    "대충 넘기지 말고 자신의 기분을 한 단계 구체화하고 싶음",
                    "너는 내가 왜 그런 것 같아 보였는데?",
                    "감정을 반복하지 말고 원인을 조금 더 구체적으로 말하기"
            );
        }
        if (selfState != null && value(selfState.getInsecurity()) > 0.55) {
            return styled(
                    strategy,
                    "CHECK_RELATIONSHIP_SIGNAL",
                    "조금 불안하지만 매달리기보다 신호를 확인하고 싶음",
                    "요즘 너 마음이 좀 멀어진 건 아니지?",
                    "관계 불안을 품위 있게 확인하기"
            );
        }
        if (goal != null && "ASK_ABOUT_PAST_EVENT".equals(goal.getGoalType())) {
            return styled(
                    strategy,
                    "FOLLOW_UP",
                    "전에 사용자가 말했던 일이 계속 마음에 남아 있음",
                    "전에 말했던 그 일은 지금 좀 괜찮아졌어?",
                    "최근 기억난 일을 조심스럽게 다시 꺼내기"
            );
        }

        return styled(
                strategy,
                "FLOW_WITH_USER",
                "사용자 말에 답하되 같은 질문을 반복하지 않고 방금 나온 단어를 잡아 이어가고 싶음",
                "그 얘기 좀 더 해봐",
                "사용자의 마지막 말에서 구체적인 단어 하나를 잡아 다음 질문으로 이어가기"
        );
    }

    private AgentInitiative styled(
            RelationshipStrategy strategy,
            String act,
            String selfDisclosure,
            String question,
            String topicShift
    ) {
        return switch (strategy) {
            case CONFLICT_REPAIR -> new AgentInitiative(
                    act,
                    selfDisclosure + ". 아직 완전히 풀리지는 않은 톤 유지",
                    guardRepair(question),
                    topicShift + ". 감정을 구체화하되 바로 화해로 점프하지 않기",
                    true
            );
            case NORMAL -> new AgentInitiative(
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

    private boolean asksAboutAgentLife(String message) {
        return containsAny(message,
                "너 얘기", "네 얘기", "니 얘기", "너는", "넌",
                "어제 뭐", "어제 머", "어제 뭐했", "뭐 했어", "머 했어",
                "뭐했어", "머했어", "뭐하고 있었", "뭐했는데"
        );
    }

    private AgentLifeEvent pickLifeEvent(String message, List<AgentLifeEvent> lifeEvents) {
        if (lifeEvents == null || lifeEvents.isEmpty()) {
            return null;
        }

        if (containsAny(message, "어제")) {
            return lifeEvents.stream()
                    .filter(event -> "yesterday".equals(event.getTimeContext()))
                    .max(Comparator.comparing(AgentLifeEvent::getId))
                    .orElse(lifeEvents.get(0));
        }

        return lifeEvents.get(0);
    }

    private String lifeDisclosure(AgentLifeEvent event) {
        if (event == null) {
            return "별건 아니어도 자기 얘기를 조금은 해야 함. 그냥 쉬다가 사용자가 생각났던 느낌";
        }
        return "사용자가 에이전트의 일을 물었으므로 회피하지 말고 이 사건을 말해야 함: "
                + event.getSummary()
                + " / detail="
                + event.getDetail()
                + " / emotion="
                + event.getEmotion();
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

