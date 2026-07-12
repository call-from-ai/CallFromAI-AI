package com.example.aidatingagentbackend.entity;

public enum RomanceStyleBand {
    MILD, SOFT, BALANCED, SPICY, EXTRA_SPICY;

    public static RomanceStyleBand from(int score) {
        if (score <= 20) return MILD;
        if (score <= 40) return SOFT;
        if (score <= 60) return BALANCED;
        if (score <= 80) return SPICY;
        return EXTRA_SPICY;
    }
}
