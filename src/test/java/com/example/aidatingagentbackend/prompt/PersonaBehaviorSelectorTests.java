package com.example.aidatingagentbackend.prompt;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PersonaBehaviorSelectorTests {
    private final PersonaBehaviorSelector selector = new PersonaBehaviorSelector();

    @Test
    void concernSelectsListenerInsteadOfFirstPlayfulKeyword() {
        assertThat(selector.selectIndex(List.of("장난기 많은", "고민을 잘 들어주는"), "회사에서 너무 힘들었어"))
                .isEqualTo(1);
    }

    @Test
    void jealousyRequiresCompetitionContext() {
        List<String> keywords = List.of("장난기 많은", "질투를 숨기지 않는");
        assertThat(selector.selectIndex(keywords, "오늘 뭐 해?")).isZero();
        assertThat(selector.selectIndex(keywords, "오늘 다른 여자랑 밥 먹었어")).isEqualTo(1);
    }

    @Test
    void concernCanActivateAffectionExpressionKeyword() {
        assertThat(selector.selectIndex(List.of("장난기 많은", "애정 표현이 많은"), "오늘 너무 힘들었어"))
                .isEqualTo(1);
    }
}
