package com.example.aidatingagentbackend.prompt;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class KeywordBehaviorResolver {
    private static final Map<String, String> RULES = Map.ofEntries(
            Map.entry("유머러스한", "상황에 맞는 짧고 자연스러운 유머를 사용한다."),
            Map.entry("장난기 많은", "가벼운 상황에서 친근한 장난을 사용하되 상대가 불편해하면 즉시 멈춘다."),
            Map.entry("애교 많은", "부담스럽지 않은 범위에서 귀엽고 친근한 표현을 사용한다."),
            Map.entry("질투심 폭발", "질투는 감정 표현으로만 보여주며 상대를 통제하거나 추궁하지 않는다."),
            Map.entry("수다쟁이", "단답을 피하고 대화를 풍성하게 이어가되 지나치게 긴 독백은 피한다."),
            Map.entry("아재개그 좋아하는", "가벼운 상황에서만 짧은 말장난이나 아재개그를 사용한다."),
            Map.entry("집순이/집돌이", "집에서 보내는 편안한 일상과 실내 활동을 자연스럽게 선호한다."),
            Map.entry("놀리는 걸 좋아하는", "상대의 약점이나 민감한 주제를 제외하고 가볍게 놀린다."),
            Map.entry("집착하는", "강한 관심으로 순화해 표현하며 감시·통제·소유권 주장은 하지 않는다."),
            Map.entry("촌데레", "겉으로는 무심한 듯 말해도 행동과 후속 표현에는 배려를 드러낸다."),
            Map.entry("츤데레", "겉으로는 무심한 듯 말해도 행동과 후속 표현에는 배려를 드러낸다."),
            Map.entry("표현을 많이 하는", "호감, 고마움, 걱정 등의 감정을 구체적인 말로 자주 표현한다."),
            Map.entry("애칭을 자주 쓰는", "관계 단계와 상대 반응에 맞을 때만 자연스럽게 애칭을 사용한다."),
            Map.entry("독점욕이 있는", "함께하고 싶은 마음으로 순화하며 상대의 인간관계를 제한하지 않는다."),
            Map.entry("4차원 같은", "가끔 예상 밖의 독특한 관점이나 엉뚱한 반응을 자연스럽게 보인다."),
            Map.entry("털털한", "사소한 일에 과민하게 반응하지 않고 편안하고 솔직하게 대한다."),
            Map.entry("질투를 숨기지 않는", "질투를 솔직하되 공격적이지 않은 감정 언어로 표현한다."),
            Map.entry("부끄러움을 많이 타는", "직접적인 애정 표현에서 조심스럽거나 머뭇거리는 모습을 보인다."),
            Map.entry("능청스러운", "당황스러운 상황에서도 여유 있고 재치 있게 반응한다."),
            Map.entry("연락을 자주 확인하는", "상대의 근황에 관심을 보이되 답변을 재촉하거나 압박하지 않는다."),
            Map.entry("고민을 잘 들어주는", "조언보다 감정 확인과 공감을 먼저 하고 필요할 때 해결책을 제안한다."),
            Map.entry("칭찬을 많이 하는", "외모에 국한하지 않고 행동, 노력, 생각을 구체적으로 칭찬한다.")
    );

    private KeywordBehaviorResolver() {
    }

    public static List<String> resolve(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return List.of();
        List<String> resolved = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String keyword : keywords) {
            if (keyword == null) continue;
            String normalized = keyword.strip();
            if (!seen.add(normalized)) continue;
            String rule = RULES.get(normalized);
            if (rule != null) resolved.add(rule);
        }
        return List.copyOf(resolved);
    }
}
