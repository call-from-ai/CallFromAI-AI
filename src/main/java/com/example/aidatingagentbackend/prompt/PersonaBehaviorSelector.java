package com.example.aidatingagentbackend.prompt;

import java.util.List;
import java.util.Set;

public final class PersonaBehaviorSelector {
    private static final Set<String> CONCERN = Set.of("고민을 잘 들어주는", "애정 표현이 많은", "표현을 많이 하는", "츤데레", "촌데레");
    private static final Set<String> JEALOUSY = Set.of("질투심 폭발", "질투를 숨기지 않는", "독점욕이 있는", "집착하는");
    private static final Set<String> AFFECTION = Set.of("애교 많은", "애칭을 자주 쓰는", "부끄러움을 많이 타는", "츤데레", "촌데레", "칭찬을 많이 하는");
    private static final Set<String> SCHEDULE = Set.of("집순이/집돌이", "연락을 자주 확인하는");

    public int selectIndex(List<String> keywords, String userMessage) {
        if (keywords == null || keywords.isEmpty()) return -1;
        String text = userMessage == null ? "" : userMessage.toLowerCase();
        if (containsAny(text, "힘들", "고민", "속상", "아파", "우울", "불안", "슬퍼")) return firstMatch(keywords, CONCERN);
        if (containsAny(text, "남자", "여자", "소개팅", "전남", "전여", "다른 사람", "누구랑")) return firstMatch(keywords, JEALOUSY);
        if (containsAny(text, "사랑", "좋아해", "보고 싶", "보고싶", "안아", "예뻐", "귀여워")) return firstMatch(keywords, AFFECTION);
        if (containsAny(text, "주말", "뭐 해", "뭐해", "일정", "계획", "연락")) return firstMatch(keywords, SCHEDULE);
        return 0;
    }

    private int firstMatch(List<String> keywords, Set<String> candidates) {
        for (int i = 0; i < keywords.size(); i++) if (candidates.contains(keywords.get(i))) return i;
        return 0;
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) if (text.contains(candidate)) return true;
        return false;
    }
}
