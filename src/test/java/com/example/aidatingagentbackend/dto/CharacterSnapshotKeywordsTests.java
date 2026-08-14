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

    @Test
    void receivesCurrentBackendMbtiWithoutLosingLegacyMindCompatibility() throws Exception {
        CharacterSnapshot current = mapper.readValue(payload(",\"mbti\":\"ENFP\",\"corePersona\":\"다정하지만 장난기 많은 사람\""), CharacterSnapshot.class);
        CharacterSnapshot legacy = mapper.readValue(payload(",\"mind\":\"무심한 듯 세심한 사람\""), CharacterSnapshot.class);

        assertThat(current.mbti()).isEqualTo("ENFP");
        assertThat(current.resolvedCorePersona()).isEqualTo("다정하지만 장난기 많은 사람");
        assertThat(legacy.resolvedCorePersona()).isEqualTo("무심한 듯 세심한 사람");
    }

    private String payload(String keywords) {
        return "{\"characterId\":10,\"name\":\"하나\",\"romanceStyleScore\":72" + keywords +
                ",\"traits\":{\"humor\":6,\"playfulness\":7,\"affection\":8,\"empathy\":9," +
                "\"attachment\":5,\"jealousy\":2,\"dominance\":4,\"confidence\":7," +
                "\"expressiveness\":8,\"emotionalStability\":7,\"calculationVersion\":1}}";
    }
}
