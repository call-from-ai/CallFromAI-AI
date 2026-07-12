package com.example.aidatingagentbackend.prompt;

import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.CharacterExample;
import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import com.example.aidatingagentbackend.entity.Memory;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTraitInstructionTests {

    private final PromptBuilder promptBuilder = new PromptBuilder(new TraitInstructionResolver());

    @Test
    void crushLimitsExcessiveRomanticExpression() {
        String prompt = baseBuilder()
                .relationshipStage(RelationshipStage.CRUSH)
                .build();

        assertThat(prompt).contains("확정적인 연인처럼 말하지 않는다");
        assertThat(prompt).contains("과한 애칭");
        assertThat(prompt).contains("no threats/coercion");
    }

    @Test
    void longTermIncludesDailyCareInstruction() {
        String prompt = baseBuilder()
                .relationshipStage(RelationshipStage.LONG_TERM)
                .build();

        assertThat(prompt).contains("일상과 일정에 대한 관심");
        assertThat(prompt).contains("현실적인 배려");
        assertThat(prompt).contains("과장된 설렘 표현을 반복하지 않는다");
    }

    @Test
    void temperatureNinetyIncludesConfidentInstruction() {
        String prompt = baseBuilder()
                .romanceStyleScore(90)
                .build();

        assertThat(prompt).contains("짧고 자신감 있는 문장");
        assertThat(prompt).contains("도발");
        assertThat(prompt).contains("매 응답을 도발적으로 만들지");
    }

    @Test
    void temperatureTenLimitsFlirting() {
        String prompt = baseBuilder()
                .romanceStyleScore(10)
                .build();

        assertThat(prompt).contains("차분하고 안정적인");
        assertThat(prompt).contains("과한 플러팅은 제한");
    }

    @Test
    void highAffectionAndLowExpressivenessAreResolved() {
        CharacterTraitProfile traits = defaultTraits();
        traits.setAffection(9);
        traits.setExpressiveness(1);

        String prompt = baseBuilder()
                .characterTraitProfile(traits)
                .build();

        assertThat(prompt).contains("애정은 많지만 직접적인 사랑 표현보다");
    }

    @Test
    void highJealousyAndHighStabilityAreResolved() {
        CharacterTraitProfile traits = defaultTraits();
        traits.setJealousy(9);
        traits.setEmotionalStability(9);

        String prompt = baseBuilder()
                .characterTraitProfile(traits)
                .build();

        assertThat(prompt).contains("질투는 느끼지만 폭발하지 않고");
    }

    @Test
    void empathyOverridesPlayfulnessInConcernContext() {
        CharacterTraitProfile traits = defaultTraits();
        traits.setPlayfulness(9);
        traits.setEmpathy(9);

        String prompt = baseBuilder()
                .characterTraitProfile(traits)
                .userMessage("나 요즘 너무 힘들고 고민 있어")
                .build();

        assertThat(prompt).contains("지금은 고민 맥락이므로 장난보다 공감을 우선한다");
    }

    @Test
    void lowEmotionDoesNotInventAngerWithHighExpressiveness() {
        CharacterTraitProfile traits = defaultTraits();
        traits.setExpressiveness(9);
        AgentSelfState selfState = new AgentSelfState();
        selfState.setAnger(0.1);

        String prompt = baseBuilder()
                .characterTraitProfile(traits)
                .agentSelfState(selfState)
                .build();

        assertThat(prompt).contains("분노가 낮으므로 화난 척을 과장하지 않는다");
    }

    @Test
    void characterExamplesAreLimitedToFiveAndStyleOnly() {
        String prompt = baseBuilder()
                .characterExamples(List.of(
                        example(1L),
                        example(2L),
                        example(3L),
                        example(4L),
                        example(5L),
                        example(6L)
                ))
                .build();

        assertThat(prompt).contains("Examples are style references only");
        assertThat(prompt).contains("A: assistant-5");
        assertThat(prompt).doesNotContain("assistant-6");
    }

    @Test
    void emptyMemoryAndExampleStillBuildsPrompt() {
        String prompt = baseBuilder()
                .memories(List.of())
                .characterExamples(List.of())
                .build();

        assertThat(prompt).contains("[User Message]");
        assertThat(prompt).contains("Rules: answer first");
    }

    private PromptBuilder.Builder baseBuilder() {
        Relationship relationship = new Relationship();
        relationship.setConflictLevel(0);
        relationship.setBreakupRisk(0);
        return promptBuilder.builder()
                .relationship(relationship)
                .characterTraitProfile(defaultTraits())
                .relationshipStage(RelationshipStage.EARLY_DATING)
                .relationshipTemperatureScore(50)
                .userMessage("안녕");
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

    private CharacterExample example(Long id) {
        CharacterExample example = new CharacterExample();
        example.setUserExample("user-" + id);
        example.setAssistantExample("assistant-" + id);
        return example;
    }
}
