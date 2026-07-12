package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseStylePostProcessorTests {

    private final ResponseStylePostProcessor processor = new ResponseStylePostProcessor();

    @Test
    void spicyLegacyPolishStillWorks() {
        String result = processor.process("뭐하고 있어?\n괜찮았어?", RelationshipTemperature.SPICY);

        assertThat(result).contains("머함");
        assertThat(result).contains("괜찮았냐");
    }

    @Test
    void conflictRepairKeepsGuardedStrategy() {
        String result = processor.process("괜찮아, 다행이야. 고마워.", RelationshipTemperature.CONFLICT_REPAIR);

        assertThat(result).contains("말은 들을게");
        assertThat(result).doesNotContain("괜찮아, 다행이야");
    }

    @Test
    void highTemperatureReducesPeriodsAndQuestionPileup() {
        String result = processor.process(
                "오늘 뭐했어?\n나 보고 싶었어?\n기다렸습니다.",
                RelationshipTemperature.NEUTRAL,
                90,
                defaultTraits(),
                RelationshipStage.EARLY_DATING,
                null
        );

        assertThat(result).contains("기다렸");
        assertThat(result).doesNotContain("기다렸습니다.");
        assertThat(result.lines().filter(line -> line.endsWith("?")).count()).isLessThanOrEqualTo(1);
    }

    @Test
    void lowTemperatureLimitsLaughs() {
        String result = processor.process(
                "ㅋㅋ 좋아ㅋㅋ 알겠어ㅋㅋ",
                RelationshipTemperature.NEUTRAL,
                10,
                defaultTraits(),
                RelationshipStage.EARLY_DATING,
                null
        );

        assertThat(count(result, "ㅋㅋ")).isLessThanOrEqualTo(1);
    }

    @Test
    void crushReplacesOverStrongPetNameWithoutAddingNewMeaning() {
        String result = processor.process(
                "자기야 오늘 왔네",
                RelationshipTemperature.NEUTRAL,
                50,
                defaultTraits(),
                RelationshipStage.CRUSH,
                null
        );

        assertThat(result).contains("너 오늘 왔네");
        assertThat(result).doesNotContain("자기야");
    }

    @Test
    void lowAngerSoftensAngryWording() {
        AgentSelfState selfState = new AgentSelfState();
        selfState.setAnger(0.1);

        String result = processor.process(
                "그건 좀 짜증나",
                RelationshipTemperature.NEUTRAL,
                50,
                defaultTraits(),
                RelationshipStage.EARLY_DATING,
                selfState
        );

        assertThat(result).contains("좀 그렇네");
    }

    private CharacterTraitProfile defaultTraits() {
        CharacterTraitProfile profile = new CharacterTraitProfile();
        profile.setExpressiveness(5);
        return profile;
    }

    private int count(String text, String token) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }
}
