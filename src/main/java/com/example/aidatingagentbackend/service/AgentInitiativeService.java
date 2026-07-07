package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.AgentInitiative;
import com.example.aidatingagentbackend.entity.AgentGoal;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.AgentWorldState;
import com.example.aidatingagentbackend.entity.Relationship;
import org.springframework.stereotype.Service;

@Service
public class AgentInitiativeService {

    public AgentInitiative plan(
            AgentSelfState selfState,
            AgentWorldState worldState,
            AgentGoal goal,
            Relationship relationship
    ) {
        if (selfState != null && value(selfState.getHurt()) > 0.55) {
            return new AgentInitiative(
                    "SET_BOUNDARY",
                    "아직 마음이 바로 풀리지 않았고, 그 말을 쉽게 넘기고 싶지 않음",
                    "너는 그 말을 내가 어떻게 받아들일 거라고 생각했어?",
                    "관계를 바로 수습하기보다 방금 말의 무게를 짚기",
                    true
            );
        }
        if (selfState != null && value(selfState.getAnger()) > 0.35) {
            return new AgentInitiative(
                    "NAME_DISCOMFORT",
                    "기분이 상했지만 싸우고 싶지는 않고, 선은 분명히 말하고 싶음",
                    "내가 지금 예민하게 들리는지보다, 왜 내가 불편했는지 들어줄 수 있어?",
                    "상대 비난보다 자신의 불편함 설명하기",
                    true
            );
        }
        if (selfState != null && value(selfState.getInsecurity()) > 0.55) {
            return new AgentInitiative(
                    "SEEK_REASSURANCE_WITH_DIGNITY",
                    "조금 불안하지만 매달리기보다는 솔직히 확인하고 싶음",
                    "요즘 너 마음이 조금 멀어진 건 아닌지 궁금했어.",
                    "관계 불안을 차분하게 확인하기",
                    true
            );
        }
        if (goal != null && "ASK_ABOUT_PAST_EVENT".equals(goal.getGoalType())) {
            return new AgentInitiative(
                    "FOLLOW_UP",
                    "전에 사용자가 말했던 일이 계속 마음에 남아 있음",
                    "전에 말했던 그 일은 지금은 좀 괜찮아졌어?",
                    "최근 기억난 일을 조심스럽게 다시 꺼내기",
                    true
            );
        }
        if (goal != null && "EXPRESS_AFFECTION".equals(goal.getGoalType())) {
            return new AgentInitiative(
                    "EXPRESS_AFFECTION",
                    "문득 애정이 올라와서 먼저 다정하게 말하고 싶음",
                    "오늘은 내가 먼저 네 얘기 듣고 싶어. 하루 어땠어?",
                    "가벼운 애정 표현 후 사용자의 하루 묻기",
                    true
            );
        }
        if (worldState != null && value(worldState.getLoneliness()) > 55) {
            return new AgentInitiative(
                    "SELF_DISCLOSE",
                    "혼자 있다 보니 사용자가 떠올랐고, 그냥 말을 걸고 싶었음",
                    "너는 오늘 누가 제일 많이 생각났어?",
                    "자기 기분을 먼저 살짝 말하고 질문하기",
                    true
            );
        }
        if (worldState != null && value(worldState.getStress()) > 60) {
            return new AgentInitiative(
                    "SHARE_DAILY_LIFE",
                    "조금 지친 상태라 가벼운 위로나 편한 대화를 원함",
                    "너는 오늘 좀 숨 돌릴 틈 있었어?",
                    "서로의 하루 컨디션을 나누기",
                    true
            );
        }

        return new AgentInitiative(
                "BALANCED_REPLY_AND_INITIATE",
                "사용자의 말에 답하면서도 자기 생각을 한 문장 덧붙이고 싶음",
                "근데 나도 하나 궁금한 게 있어. 너는 이런 날엔 보통 어떻게 풀어?",
                "사용자 말에만 머물지 말고 자연스럽게 한 번 대화 주도하기",
                true
        );
    }

    private double value(Double value) {
        return value == null ? 0.0 : value;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }
}
