package com.example.aidatingagentbackend.prompt;

import com.example.aidatingagentbackend.dto.CharacterSnapshot;
import com.example.aidatingagentbackend.dto.CharacterTraitSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PersonaPolicyRegressionTests {
    private final PromptBuilder promptBuilder = new PromptBuilder(
            new TraitInstructionResolver(), new RomanceStylePromptResolver(), new KoreanAddressResolver());

    @Test
    void explicitTsundereIdentitySurvivesNeutralQuantitativeTraits() {
        CharacterSnapshot character = character(List.of("츤데레"));

        String prompt = promptBuilder.builder().character(character).userMessage("나 오늘 좀 아파").build();

        assertThat(prompt)
                .contains("selected persona behavior > trait tuning")
                .contains("첫 반응은 살짝 무심하거나 퉁명스럽게")
                .contains("같은 답변 안에서 챙김이나 걱정")
                .contains("Show it observably in this reply");
    }

    @Test
    void keywordOrderRemainsPersonaPriorityOrder() {
        String prompt = promptBuilder.builder()
                .character(character(List.of("능청스러운", "부끄러움을 많이 타는")))
                .build();

        assertThat(prompt.indexOf("Active behavior: 당황하거나 놀림받아도"))
                .isLessThan(prompt.indexOf("Supporting: 직접적인 애정 표현 앞에서는"));
    }

    private CharacterSnapshot character(List<String> keywords) {
        return new CharacterSnapshot(10L, "하나", null, "CASUAL", "개발자", null, null, 50,
                keywords, 25, "FEMALE", "무심한 척하지만 세심하게 챙기는 사람", "ENFP",
                new CharacterTraitSnapshot(5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 2));
    }
}
