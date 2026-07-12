package com.example.aidatingagentbackend.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CharacterTraitsRequestJsonTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void invalidPersonalityKeywordFailsJsonBinding() {
        String json = "{\"personalityKeywords\":[\"NOT_A_REAL_KEYWORD\"]}";

        assertThatThrownBy(() -> objectMapper.readValue(json, CharacterTraitsRequest.class))
                .isInstanceOf(Exception.class);
    }
}
