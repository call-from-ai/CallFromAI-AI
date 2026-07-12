package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.domain.CharacterTrait;
import com.example.aidatingagentbackend.dto.PersonalityTraitSelection;
import com.example.aidatingagentbackend.entity.Mbti;
import com.example.aidatingagentbackend.entity.PersonalityKeyword;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PersonalityTraitResolver {

    private static final double[] PRIORITY_WEIGHTS = {0, 1.0, 0.9, 0.8, 0.7, 0.6};
    private static final Map<PersonalityKeyword, Map<TraitKey, Double>> KEYWORD_DELTAS = policy();

    public CharacterTrait resolve(Set<PersonalityKeyword> keywords) {
        if (keywords == null) return CharacterTrait.defaults();
        int[] priority = {1};
        return resolve(keywords.stream()
                .map(keyword -> new PersonalityTraitSelection(keyword, Math.min(priority[0]++, 5)))
                .toList(), null);
    }

    public CharacterTrait resolve(List<PersonalityTraitSelection> selections, Mbti mbti) {
        Map<TraitKey, List<Double>> positive = buckets();
        Map<TraitKey, List<Double>> negative = buckets();
        if (selections != null) {
            for (PersonalityTraitSelection selection : selections) {
                if (selection == null || selection.getTrait() == null || selection.getPriority() == null) continue;
                double weight = priorityWeight(selection.getPriority());
                KEYWORD_DELTAS.get(selection.getTrait()).forEach((trait, delta) ->
                        (delta >= 0 ? positive : negative).get(trait).add(delta * weight));
            }
        }

        EnumMap<TraitKey, Double> values = new EnumMap<>(TraitKey.class);
        for (TraitKey key : TraitKey.values()) {
            values.put(key, CharacterTrait.DEFAULT_VALUE
                    + diminished(positive.get(key)) - diminishedAbsolute(negative.get(key)));
        }
        applyMbti(values, mbti);
        return new CharacterTrait(
                rounded(values.get(TraitKey.HUMOR)), rounded(values.get(TraitKey.PLAYFULNESS)),
                rounded(values.get(TraitKey.AFFECTION)), rounded(values.get(TraitKey.EMPATHY)),
                rounded(values.get(TraitKey.ATTACHMENT)), rounded(values.get(TraitKey.JEALOUSY)),
                rounded(values.get(TraitKey.DOMINANCE)), rounded(values.get(TraitKey.CONFIDENCE)),
                rounded(values.get(TraitKey.EXPRESSIVENESS)), rounded(values.get(TraitKey.EMOTIONAL_STABILITY)));
    }

    private static double priorityWeight(int priority) {
        if (priority < 1 || priority > 5) throw new IllegalArgumentException("priority must be between 1 and 5");
        return PRIORITY_WEIGHTS[priority];
    }

    private static double diminished(List<Double> deltas) {
        deltas.sort((a, b) -> Double.compare(Math.abs(b), Math.abs(a)));
        double total = 0;
        for (int i = 0; i < deltas.size(); i++) total += deltas.get(i) * (i == 0 ? 1 : i == 1 ? .75 : .5);
        return total;
    }

    private static double diminishedAbsolute(List<Double> deltas) {
        List<Double> absolute = new ArrayList<>();
        deltas.forEach(value -> absolute.add(Math.abs(value)));
        return diminished(absolute);
    }

    private static int rounded(double value) {
        return Math.max(0, Math.min(10, (int) Math.round(value)));
    }

    private static Map<TraitKey, List<Double>> buckets() {
        EnumMap<TraitKey, List<Double>> result = new EnumMap<>(TraitKey.class);
        for (TraitKey key : TraitKey.values()) result.put(key, new ArrayList<>());
        return result;
    }

    private static void applyMbti(Map<TraitKey, Double> v, Mbti mbti) {
        if (mbti == null) return;
        String type = mbti.name();
        add(v, TraitKey.EXPRESSIVENESS, type.charAt(0) == 'E' ? 1 : -1);
        add(v, type.charAt(0) == 'E' ? TraitKey.PLAYFULNESS : TraitKey.EMPATHY, .5);
        add(v, type.charAt(0) == 'E' ? TraitKey.DOMINANCE : TraitKey.ATTACHMENT, .5);
        if (type.charAt(1) == 'N') { add(v, TraitKey.HUMOR, .5); add(v, TraitKey.PLAYFULNESS, .5); }
        else { add(v, TraitKey.EMPATHY, .5); add(v, TraitKey.EMOTIONAL_STABILITY, .5); }
        if (type.charAt(2) == 'T') { add(v, TraitKey.EMOTIONAL_STABILITY, .5); add(v, TraitKey.EMPATHY, -.5); }
        else { add(v, TraitKey.EMPATHY, 1); add(v, TraitKey.EXPRESSIVENESS, .5); }
        if (type.charAt(3) == 'J') { add(v, TraitKey.DOMINANCE, .5); add(v, TraitKey.EMOTIONAL_STABILITY, .5); }
        else { add(v, TraitKey.PLAYFULNESS, .5); add(v, TraitKey.HUMOR, .5); }
    }

    private static void add(Map<TraitKey, Double> values, TraitKey key, double delta) {
        values.put(key, values.get(key) + delta);
    }

    private static Map<PersonalityKeyword, Map<TraitKey, Double>> policy() {
        EnumMap<PersonalityKeyword, Map<TraitKey, Double>> p = new EnumMap<>(PersonalityKeyword.class);
        p.put(PersonalityKeyword.HUMOROUS, d(TraitKey.HUMOR,7,TraitKey.PLAYFULNESS,3,TraitKey.EMOTIONAL_STABILITY,1));
        p.put(PersonalityKeyword.PLAYFUL, d(TraitKey.PLAYFULNESS,7,TraitKey.HUMOR,3,TraitKey.CONFIDENCE,2));
        p.put(PersonalityKeyword.CUTE, d(TraitKey.AFFECTION,7,TraitKey.EXPRESSIVENESS,6,TraitKey.DOMINANCE,-3));
        p.put(PersonalityKeyword.HIGH_JEALOUSY, d(TraitKey.JEALOUSY,8,TraitKey.ATTACHMENT,5,TraitKey.EMOTIONAL_STABILITY,-6,TraitKey.CONFIDENCE,-1));
        p.put(PersonalityKeyword.TALKATIVE, d(TraitKey.EXPRESSIVENESS,8,TraitKey.AFFECTION,2,TraitKey.DOMINANCE,1));
        p.put(PersonalityKeyword.DAD_JOKE, d(TraitKey.HUMOR,7,TraitKey.CONFIDENCE,3,TraitKey.PLAYFULNESS,2));
        p.put(PersonalityKeyword.HOMEBODY, d(TraitKey.EMOTIONAL_STABILITY,4,TraitKey.AFFECTION,2));
        p.put(PersonalityKeyword.TEASING, d(TraitKey.PLAYFULNESS,7,TraitKey.DOMINANCE,4,TraitKey.CONFIDENCE,4,TraitKey.EMPATHY,-2));
        p.put(PersonalityKeyword.CLINGY, d(TraitKey.ATTACHMENT,8,TraitKey.JEALOUSY,5,TraitKey.EMOTIONAL_STABILITY,-7,TraitKey.CONFIDENCE,-2));
        p.put(PersonalityKeyword.TSUNDERE, d(TraitKey.AFFECTION,5,TraitKey.EXPRESSIVENESS,-7,TraitKey.CONFIDENCE,3,TraitKey.DOMINANCE,1));
        p.put(PersonalityKeyword.EXPRESSIVE, d(TraitKey.EXPRESSIVENESS,8,TraitKey.AFFECTION,4,TraitKey.EMOTIONAL_STABILITY,-1));
        p.put(PersonalityKeyword.NICKNAME_LOVER, d(TraitKey.AFFECTION,6,TraitKey.EXPRESSIVENESS,5));
        p.put(PersonalityKeyword.POSSESSIVE, d(TraitKey.ATTACHMENT,7,TraitKey.JEALOUSY,7,TraitKey.DOMINANCE,4,TraitKey.EMOTIONAL_STABILITY,-5,TraitKey.EMPATHY,-2));
        p.put(PersonalityKeyword.QUIRKY, d(TraitKey.HUMOR,5,TraitKey.PLAYFULNESS,5,TraitKey.CONFIDENCE,1));
        p.put(PersonalityKeyword.EASY_GOING, d(TraitKey.EMOTIONAL_STABILITY,8,TraitKey.JEALOUSY,-6,TraitKey.ATTACHMENT,-4,TraitKey.DOMINANCE,-2));
        p.put(PersonalityKeyword.OPENLY_JEALOUS, d(TraitKey.JEALOUSY,7,TraitKey.EXPRESSIVENESS,7,TraitKey.EMOTIONAL_STABILITY,-4));
        p.put(PersonalityKeyword.SHY, d(TraitKey.CONFIDENCE,-8,TraitKey.EXPRESSIVENESS,-6,TraitKey.DOMINANCE,-4,TraitKey.AFFECTION,3,TraitKey.EMPATHY,1));
        p.put(PersonalityKeyword.SMOOTH, d(TraitKey.CONFIDENCE,8,TraitKey.HUMOR,4,TraitKey.PLAYFULNESS,4,TraitKey.EMOTIONAL_STABILITY,1));
        p.put(PersonalityKeyword.FREQUENT_CONTACT_CHECKER, d(TraitKey.ATTACHMENT,7,TraitKey.AFFECTION,3,TraitKey.EMOTIONAL_STABILITY,-3));
        p.put(PersonalityKeyword.GOOD_LISTENER, d(TraitKey.EMPATHY,8,TraitKey.EMOTIONAL_STABILITY,4,TraitKey.DOMINANCE,-2,TraitKey.EXPRESSIVENESS,-1));
        p.put(PersonalityKeyword.COMPLIMENT_GIVER, d(TraitKey.AFFECTION,5,TraitKey.EXPRESSIVENESS,5,TraitKey.EMPATHY,3));
        return Map.copyOf(p);
    }

    private static Map<TraitKey, Double> d(Object... values) {
        EnumMap<TraitKey, Double> result = new EnumMap<>(TraitKey.class);
        for (int i = 0; i < values.length; i += 2) result.put((TraitKey) values[i], ((Number) values[i + 1]).doubleValue());
        return Map.copyOf(result);
    }

    private enum TraitKey { HUMOR, PLAYFULNESS, AFFECTION, EMPATHY, ATTACHMENT, JEALOUSY, DOMINANCE, CONFIDENCE, EXPRESSIVENESS, EMOTIONAL_STABILITY }
}
