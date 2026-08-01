package com.example.aidatingagentbackend.prompt;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RomanceStylePromptResolverTests {

    private final RomanceStylePromptResolver resolver = new RomanceStylePromptResolver();

    @ParameterizedTest
    @CsvSource({
            "0, MILD",
            "20, MILD",
            "21, SOFT",
            "40, SOFT",
            "41, BALANCED",
            "60, BALANCED",
            "61, SPICY",
            "80, SPICY",
            "81, EXTRA_SPICY",
            "100, EXTRA_SPICY"
    })
    void resolvesScoreBoundariesToExpectedBand(int score, String band) {
        assertThat(resolver.resolve(score)).startsWith("[Romance Style: " + band + "]");
    }

    @ParameterizedTest
    @CsvSource({
            "-1, MILD",
            "101, EXTRA_SPICY"
    })
    void clampsScoresOutsideSupportedRange(int score, String band) {
        assertThat(resolver.resolve(score)).startsWith("[Romance Style: " + band + "]");
    }
}
