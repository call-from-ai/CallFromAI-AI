package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.domain.CharacterTrait;
import com.example.aidatingagentbackend.entity.PersonalityKeyword;
import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import com.example.aidatingagentbackend.entity.Mbti;
import com.example.aidatingagentbackend.dto.PersonalityTraitSelection;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonalityTraitResolverTests {

    private final PersonalityTraitResolver resolver = new PersonalityTraitResolver();
    private final RelationshipTemperatureScoreResolver temperatureResolver =
            new RelationshipTemperatureScoreResolver(new SettingsDefaultPolicy());

    @Test
    void emptyKeywordsReturnDefaultTraits() {
        CharacterTrait trait = resolver.resolve(Set.of());

        assertAllDefault(trait);
    }

    @Test
    void nullKeywordsReturnDefaultTraits() {
        CharacterTrait trait = resolver.resolve(null);

        assertAllDefault(trait);
    }

    @Test
    void humorousIncreasesHumorAndPlayfulness() {
        CharacterTrait trait = resolver.resolve(Set.of(PersonalityKeyword.HUMOROUS));

        assertThat(trait.humor()).isEqualTo(10);
        assertThat(trait.playfulness()).isEqualTo(6);
    }

    @Test
    void clingyIncreasesAttachmentAndLowersEmotionalStability() {
        CharacterTrait trait = resolver.resolve(Set.of(PersonalityKeyword.CLINGY));

        assertThat(trait.attachment()).isBetween(0, 10);
        assertThat(trait.jealousy()).isEqualTo(8);
        assertThat(trait.emotionalStability()).isEqualTo(0);
    }

    @Test
    void multipleKeywordsAreSummed() {
        CharacterTrait trait = resolver.resolve(Set.of(
                PersonalityKeyword.GOOD_LISTENER,
                PersonalityKeyword.COMPLIMENT_GIVER
        ));

        assertThat(trait.empathy()).isEqualTo(10);
        assertThat(trait.affection()).isGreaterThanOrEqualTo(7);
        assertThat(trait.expressiveness()).isGreaterThanOrEqualTo(6);
        assertThat(trait.emotionalStability()).isGreaterThanOrEqualTo(6);
    }

    @Test
    void resultsAreClampedToZeroAndTen() {
        CharacterTrait trait = resolver.resolve(Set.of(
                PersonalityKeyword.HUMOROUS,
                PersonalityKeyword.PLAYFUL,
                PersonalityKeyword.DAD_JOKE,
                PersonalityKeyword.SMOOTH,
                PersonalityKeyword.CLINGY,
                PersonalityKeyword.SHY
        ));

        assertThat(trait.humor()).isEqualTo(10);
        assertThat(trait.playfulness()).isEqualTo(10);
        assertThat(trait.attachment()).isBetween(0, 10);
        assertThat(trait.confidence()).isBetween(0, 10);
        assertThat(trait.emotionalStability()).isBetween(0, 10);
    }

    @Test
    void duplicateKeywordsAreAppliedOnce() {
        LinkedHashSet<PersonalityKeyword> keywords = new LinkedHashSet<>();
        keywords.add(PersonalityKeyword.HUMOROUS);
        keywords.add(PersonalityKeyword.HUMOROUS);

        CharacterTrait trait = resolver.resolve(keywords);

        assertThat(trait.humor()).isEqualTo(10);
        assertThat(trait.playfulness()).isEqualTo(6);
    }

    @Test
    void relationshipTemperatureScoreBelowZeroFailsValidation() {
        assertThatThrownBy(() -> temperatureResolver.validate(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void relationshipTemperatureScoreAboveOneHundredFailsValidation() {
        assertThatThrownBy(() -> temperatureResolver.validate(101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stageAndTemperatureDoNotAffectKeywordTraitResult() {
        CharacterTrait crushLowTemp = resolver.resolve(Set.of(PersonalityKeyword.CUTE));
        temperatureResolver.validate(10);

        CharacterTrait longTermHighTemp = resolver.resolve(Set.of(PersonalityKeyword.CUTE));
        temperatureResolver.validate(90);

        assertThat(crushLowTemp).isEqualTo(longTermHighTemp);
    }

    @Test
    void existingRelationshipTemperatureEnumStillCompiles() {
        RelationshipTemperature temperature = RelationshipTemperature.CONFLICT_REPAIR;

        assertThat(temperature).isEqualTo(RelationshipTemperature.CONFLICT_REPAIR);
        assertThat(temperatureResolver.defaultScoreForLegacy(temperature)).isEqualTo(50);
    }

    private void assertAllDefault(CharacterTrait trait) {
        assertThat(trait.humor()).isEqualTo(3);
        assertThat(trait.playfulness()).isEqualTo(3);
        assertThat(trait.affection()).isEqualTo(3);
        assertThat(trait.empathy()).isEqualTo(3);
        assertThat(trait.attachment()).isEqualTo(3);
        assertThat(trait.jealousy()).isEqualTo(3);
        assertThat(trait.dominance()).isEqualTo(3);
        assertThat(trait.confidence()).isEqualTo(3);
        assertThat(trait.expressiveness()).isEqualTo(3);
        assertThat(trait.emotionalStability()).isEqualTo(3);
    }

    @Test
    void priorityOrderChangesConflictingExpressiveness() {
        CharacterTrait shyFirst = resolver.resolve(List.of(
                new PersonalityTraitSelection(PersonalityKeyword.SHY, 1),
                new PersonalityTraitSelection(PersonalityKeyword.EXPRESSIVE, 2)), null);
        CharacterTrait expressiveFirst = resolver.resolve(List.of(
                new PersonalityTraitSelection(PersonalityKeyword.EXPRESSIVE, 1),
                new PersonalityTraitSelection(PersonalityKeyword.SHY, 2)), null);
        assertThat(shyFirst.expressiveness()).isNotEqualTo(expressiveFirst.expressiveness());
    }

    @Test
    void mbtiIsWeakAndDoesNotReverseShyDirection() {
        List<PersonalityTraitSelection> humorous = List.of(new PersonalityTraitSelection(PersonalityKeyword.HUMOROUS, 1));
        CharacterTrait extrovert = resolver.resolve(humorous, Mbti.ENFJ);
        CharacterTrait introvert = resolver.resolve(humorous, Mbti.INFJ);
        assertThat(extrovert.expressiveness()).isGreaterThan(introvert.expressiveness());
        CharacterTrait shy = resolver.resolve(List.of(new PersonalityTraitSelection(PersonalityKeyword.SHY, 1)), Mbti.ENFJ);
        assertThat(shy.confidence()).isLessThan(CharacterTrait.DEFAULT_VALUE);
    }

    @Test
    void romanceStyleIsNotATraitInput() {
        CharacterTrait before = resolver.resolve(Set.of(PersonalityKeyword.GOOD_LISTENER));
        temperatureResolver.validate(90);
        assertThat(resolver.resolve(Set.of(PersonalityKeyword.GOOD_LISTENER))).isEqualTo(before);
        assertThat(before.jealousy()).isEqualTo(3);
    }
}
