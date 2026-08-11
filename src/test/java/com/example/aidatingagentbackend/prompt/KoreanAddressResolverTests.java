package com.example.aidatingagentbackend.prompt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KoreanAddressResolverTests {
    private final KoreanAddressResolver resolver = new KoreanAddressResolver();

    @Test
    void resolvesAllFourKoreanOlderSiblingTerms() {
        assertThat(resolver.resolve(30, "FEMALE", 25, "MALE").term()).isEqualTo("누나");
        assertThat(resolver.resolve(30, "MALE", 25, "MALE").term()).isEqualTo("형");
        assertThat(resolver.resolve(30, "FEMALE", 25, "FEMALE").term()).isEqualTo("언니");
        assertThat(resolver.resolve(30, "MALE", 25, "FEMALE").term()).isEqualTo("오빠");
    }

    @Test
    void doesNotGuessWhenCharacterIsNotYoungerOrProfileIsIncomplete() {
        assertThat(resolver.resolve(25, "MALE", 25, "FEMALE").shouldUseKinshipTerm()).isFalse();
        assertThat(resolver.resolve(25, null, 20, "FEMALE").shouldUseKinshipTerm()).isFalse();
    }
}
