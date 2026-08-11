package com.example.aidatingagentbackend.prompt;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordBehaviorResolverTests {
    @Test
    void knownKeywordsBecomeBehaviorRulesInInputOrder() {
        assertThat(KeywordBehaviorResolver.resolve(List.of("장난기 많은", "고민을 잘 들어주는")))
                .containsExactly(
                        "가벼운 상황에서 친근한 장난을 사용하되 상대가 불편해하면 즉시 멈춘다.",
                        "조언보다 감정 확인과 공감을 먼저 하고 필요할 때 해결책을 제안한다.");
    }

    @Test
    void unknownAndDuplicateKeywordsAreIgnored() {
        assertThat(KeywordBehaviorResolver.resolve(
                List.of("장난기 많은", "정의되지 않은 키워드", "장난기 많은")))
                .containsExactly("가벼운 상황에서 친근한 장난을 사용하되 상대가 불편해하면 즉시 멈춘다.");
    }
}
