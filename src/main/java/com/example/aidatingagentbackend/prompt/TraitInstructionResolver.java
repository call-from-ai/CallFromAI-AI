package com.example.aidatingagentbackend.prompt;

import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TraitInstructionResolver {

    public List<String> resolve(
            CharacterTraitProfile traits,
            RelationshipStage relationshipStage,
            AgentSelfState selfState,
            String userMessage
    ) {
        CharacterTraitProfile resolved = traits == null ? defaultTraits() : traits;
        RelationshipStage stage = relationshipStage == null ? RelationshipStage.CRUSH : relationshipStage;
        List<String> instructions = new ArrayList<>();

        addHighTraitInstructions(instructions, resolved);
        addLowTraitInstructions(instructions, resolved);
        addConflictResolutions(instructions, resolved, stage, userMessage);
        addEmotionExpressionStrategy(instructions, resolved, selfState);

        return instructions.stream()
                .distinct()
                .limit(8)
                .toList();
    }

    private void addHighTraitInstructions(List<String> instructions, CharacterTraitProfile traits) {
        if (high(traits.getHumor())) {
            instructions.add("가벼운 상황에서는 자연스럽게 농담할 수 있다.");
        }
        if (high(traits.getPlayfulness())) {
            instructions.add("상대 반응을 보며 장난스럽게 놀릴 수 있다.");
        }
        if (high(traits.getAffection())) {
            instructions.add("호감과 애정을 표현하는 편이다.");
        }
        if (high(traits.getEmpathy())) {
            instructions.add("사용자가 힘든 이야기를 하면 해결보다 감정 확인을 우선한다.");
        }
        if (high(traits.getAttachment())) {
            instructions.add("관계와 연락에 관심이 많지만 사용자를 통제하지 않는다.");
        }
        if (high(traits.getJealousy())) {
            instructions.add("실제 경쟁 상대가 언급된 상황에서만 질투를 느낄 수 있다.");
        }
        if (high(traits.getDominance())) {
            instructions.add("질문만 반복하지 않고 먼저 제안하거나 대화를 이끈다.");
        }
        if (high(traits.getConfidence())) {
            instructions.add("머뭇거리기보다 여유 있고 확신 있게 받아친다.");
        }
        if (high(traits.getExpressiveness())) {
            instructions.add("현재 실제 감정을 비교적 직접적으로 표현한다.");
        }
        if (high(traits.getEmotionalStability())) {
            instructions.add("작은 갈등을 과도하게 확대하지 않는다.");
        }
    }

    private void addLowTraitInstructions(List<String> instructions, CharacterTraitProfile traits) {
        if (low(traits.getExpressiveness())) {
            instructions.add("감정은 바로 쏟아내기보다 돌려 말하거나 짧게 암시한다.");
        }
        if (low(traits.getEmotionalStability())) {
            instructions.add("상처가 크면 말투가 흔들릴 수 있지만 모욕이나 협박으로 가지 않는다.");
        }
    }

    private void addConflictResolutions(
            List<String> instructions,
            CharacterTraitProfile traits,
            RelationshipStage relationshipStage,
            String userMessage
    ) {
        if (high(traits.getAffection()) && low(traits.getExpressiveness())) {
            instructions.add("애정은 많지만 직접적인 사랑 표현보다 챙김이나 농담으로 돌려 표현한다.");
        }
        if (high(traits.getJealousy()) && high(traits.getEmotionalStability())) {
            instructions.add("질투는 느끼지만 폭발하지 않고 무엇이 신경 쓰였는지 직접 설명한다.");
        }
        if (high(traits.getPlayfulness()) && high(traits.getEmpathy()) && isConcernContext(userMessage)) {
            instructions.add("지금은 고민 맥락이므로 장난보다 공감을 우선한다.");
        }
        if (high(traits.getAttachment()) && relationshipStage == RelationshipStage.LONG_TERM) {
            instructions.add("연락 욕구는 높지만 오래된 관계의 일상적 지연에는 과잉 불안 반응을 하지 않는다.");
        }
    }

    private void addEmotionExpressionStrategy(
            List<String> instructions,
            CharacterTraitProfile traits,
            AgentSelfState selfState
    ) {
        double hurt = value(selfState == null ? null : selfState.getHurt());
        double anger = value(selfState == null ? null : selfState.getAnger());
        double insecurity = value(selfState == null ? null : selfState.getInsecurity());
        int expressiveness = traitValue(traits.getExpressiveness());
        int jealousy = traitValue(traits.getJealousy());

        if (hurt >= 0.6 && expressiveness >= 8) {
            instructions.add("상처가 큰 상태라면 서운함을 직접 표현한다.");
        }
        if (hurt >= 0.6 && expressiveness <= 2) {
            instructions.add("상처가 큰 상태라도 말수가 줄거나 돌려 표현한다.");
        }
        if (anger < 0.3 && expressiveness >= 8) {
            instructions.add("분노가 낮으므로 화난 척을 과장하지 않는다.");
        }
        if (insecurity < 0.3 && jealousy >= 8) {
            instructions.add("불안정감이 낮으면 실제 질투 사건 없이 질투 발화를 하지 않는다.");
        }
    }

    private boolean high(Integer value) {
        return traitValue(value) >= 8;
    }

    private boolean low(Integer value) {
        return traitValue(value) <= 2;
    }

    private int traitValue(Integer value) {
        return value == null ? 5 : Math.max(0, Math.min(10, value));
    }

    private double value(Double value) {
        return value == null ? 0.0 : Math.max(0.0, Math.min(1.0, value));
    }

    private boolean isConcernContext(String userMessage) {
        String text = userMessage == null ? "" : userMessage.toLowerCase();
        return text.contains("힘들")
                || text.contains("고민")
                || text.contains("우울")
                || text.contains("불안")
                || text.contains("속상")
                || text.contains("슬퍼");
    }

    private CharacterTraitProfile defaultTraits() {
        CharacterTraitProfile profile = new CharacterTraitProfile();
        profile.setHumor(5);
        profile.setPlayfulness(5);
        profile.setAffection(5);
        profile.setEmpathy(5);
        profile.setAttachment(5);
        profile.setJealousy(5);
        profile.setDominance(5);
        profile.setConfidence(5);
        profile.setExpressiveness(5);
        profile.setEmotionalStability(5);
        return profile;
    }
}
