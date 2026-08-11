package com.example.aidatingagentbackend.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CharacterSnapshotKeywordsTests {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void omittedKeywordsDefaultToEmptyList() throws Exception {
        CharacterSnapshot snapshot = mapper.readValue(payload(""), CharacterSnapshot.class);
        assertThat(snapshot.keywords()).isEmpty();
    }

    @Test
    void nullKeywordsNormalizeToEmptyList() throws Exception {
        CharacterSnapshot snapshot = mapper.readValue(payload(",\"keywords\":null"), CharacterSnapshot.class);
        assertThat(snapshot.keywords()).isEmpty();
    }

    private String payload(String keywords) {
        return "{\"characterId\":10,\"name\":\"하나\",\"romanceStyleScore\":72" + keywords +
                ",\"traits\":{\"humor\":6,\"playfulness\":7,\"affection\":8,\"empathy\":9," +
                "\"attachment\":5,\"jealousy\":2,\"dominance\":4,\"confidence\":7," +
                "\"expressiveness\":8,\"emotionalStability\":7,\"calculationVersion\":1}}";
    }
}
