package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.PreferenceQuestionPlan;
import com.example.aidatingagentbackend.entity.CharacterPreference;
import com.example.aidatingagentbackend.repository.CharacterPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CharacterPreferenceService {

    private final CharacterPreferenceRepository characterPreferenceRepository;

    public CharacterPreferenceService(CharacterPreferenceRepository characterPreferenceRepository) {
        this.characterPreferenceRepository = characterPreferenceRepository;
    }

    @Transactional(readOnly = true)
    public PreferenceQuestionPlan plan(Long characterId, String userMessage) {
        String key = detectPreferenceKey(userMessage);
        if (key == null) {
            return new PreferenceQuestionPlan("NONE", null, "none", null, null, null);
        }

        return characterPreferenceRepository.findByCharacterIdAndPreferenceKey(characterId, key)
                .map(preference -> new PreferenceQuestionPlan(
                        "character_preference",
                        key,
                        "use_known",
                        preference.getPreferenceValue(),
                        null,
                        "Use the known preference consistently. Answer first, then ask at most one follow-up."
                ))
                .orElseGet(() -> new PreferenceQuestionPlan(
                        "character_preference",
                        key,
                        "invent_and_persist",
                        null,
                        defaultHint(key),
                        "Invent one concrete preference that fits the character core profile. Do not dodge. Do not say you do not know."
                ));
    }

    @Transactional(readOnly = true)
    public List<CharacterPreference> findForPrompt(Long characterId) {
        return characterPreferenceRepository.findTop12ByCharacterIdOrderByUpdatedAtDescIdDesc(characterId);
    }

    @Transactional(readOnly = true)
    public List<CharacterPreference> findForPrompt(Long characterId, PreferenceQuestionPlan plan) {
        if (plan != null && plan.preferenceKey() != null) {
            return characterPreferenceRepository.findByCharacterIdAndPreferenceKey(characterId, plan.preferenceKey())
                    .map(List::of)
                    .orElse(List.of());
        }

        return List.of();
    }

    @Transactional
    public void persistInventedPreferenceIfNeeded(
            Long characterId,
            String userMessage,
            String assistantReply,
            PreferenceQuestionPlan plan
    ) {
        if (plan == null || !"invent_and_persist".equals(plan.action()) || plan.preferenceKey() == null) {
            return;
        }
        if (characterPreferenceRepository.findByCharacterIdAndPreferenceKey(characterId, plan.preferenceKey()).isPresent()) {
            return;
        }

        CharacterPreference preference = new CharacterPreference();
        preference.setCharacterId(characterId);
        preference.setPreferenceKey(plan.preferenceKey());
        preference.setPreferenceValue(extractPreferenceValue(plan.preferenceKey(), assistantReply, plan.inventionHint()));
        preference.setSource("invented_in_conversation");
        preference.setConfidence(0.72);
        preference.setStability("medium");
        preference.setCreatedFromMessage(userMessage);
        characterPreferenceRepository.save(preference);
    }

    private String detectPreferenceKey(String userMessage) {
        String message = normalize(userMessage);
        if (!containsAny(message, "좋아", "취향", "뭐 들어", "뭐듣", "뭐 먹", "뭐먹", "어떤", "추천")) {
            return null;
        }
        if (containsAny(message, "노래", "음악", "플리", "가수", "k-pop", "케이팝")) {
            return "music.genre";
        }
        if (containsAny(message, "음식", "메뉴", "뭐 먹", "뭐먹", "밥", "디저트", "카페")) {
            return "food.preference";
        }
        if (containsAny(message, "장소", "어디", "데이트", "카페", "산책")) {
            return "place.preference";
        }
        if (containsAny(message, "영화", "드라마", "애니", "영상")) {
            return "media.preference";
        }
        if (containsAny(message, "취미", "쉴 때", "쉬면", "주말")) {
            return "hobby.preference";
        }
        return "general.preference";
    }

    private String defaultHint(String key) {
        return switch (key) {
            case "music.genre" -> "잔잔한 R&B, 인디, 밤에 듣기 좋은 음악. 약간 축축한 분위기.";
            case "food.preference" -> "아이스크림처럼 이상한 수식어가 붙으면 어색한 음식은 피하고, 떡볶이/라면/초밥/김치볶음밥/치킨처럼 자연스러운 음식 하나를 고르기.";
            case "place.preference" -> "시끄러운 곳보다 밤 산책, 작은 카페, 조용한 골목 같은 장소.";
            case "media.preference" -> "밝기만 한 작품보다 여운 남는 영화/드라마/플레이리스트.";
            case "hobby.preference" -> "누워서 음악 듣기, 밤 산책, 별 의미 없는 영상 보기처럼 가벼운 취미.";
            default -> "캐릭터 성격과 모순되지 않는 구체적인 취향 하나.";
        };
    }

    private String extractPreferenceValue(String key, String assistantReply, String fallback) {
        if (assistantReply == null || assistantReply.isBlank()) {
            return fallback;
        }

        String compact = assistantReply.replaceAll("\\s+", " ").strip();
        if ("food.preference".equals(key) && compact.contains("차가운 아이스크림")) {
            return "아이스크림이나 떡볶이처럼 기분 따라 먹는 음식. 어색하게 꾸미기보다 그냥 구체적인 음식명을 말함.";
        }
        if (compact.length() > 160) {
            compact = compact.substring(0, 160).strip();
        }
        return compact;
    }

    private boolean containsAny(String message, String... patterns) {
        if (message == null || message.isBlank()) {
            return false;
        }
        for (String pattern : patterns) {
            if (message.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String message) {
        return message == null ? "" : message.toLowerCase();
    }
}
