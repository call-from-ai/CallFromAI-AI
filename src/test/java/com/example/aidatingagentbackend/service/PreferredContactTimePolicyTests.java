package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.entity.PreferTime;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.TimeZone;

import static com.example.aidatingagentbackend.service.ProactiveSchedulingService.PreferredTimeStatus.NOT_PREFERRED;
import static com.example.aidatingagentbackend.service.ProactiveSchedulingService.PreferredTimeStatus.PREFERRED;
import static org.assertj.core.api.Assertions.assertThat;

class PreferredContactTimePolicyTests {
    private final PreferredContactTimePolicy policy = new PreferredContactTimePolicy();
    private final ZoneId seoul = ZoneId.of("Asia/Seoul");

    @Test void morningInsideAndOutside() { assertStatus(PreferTime.MORNING, "2026-07-19T00:00:00Z", PREFERRED); assertStatus(PreferTime.MORNING, "2026-07-19T04:00:00Z", NOT_PREFERRED); }
    @Test void dayInsideAndOutside() { assertStatus(PreferTime.DAY, "2026-07-19T04:00:00Z", PREFERRED); assertStatus(PreferTime.DAY, "2026-07-19T10:00:00Z", NOT_PREFERRED); }
    @Test void lateEveningInsideAndOutside() { assertStatus(PreferTime.LATE_EVENING, "2026-07-19T12:00:00Z", PREFERRED); assertStatus(PreferTime.LATE_EVENING, "2026-07-19T04:00:00Z", NOT_PREFERRED); }

    @Test
    void calculatesSameDayAndNextDayStarts() {
        assertThat(policy.evaluate(PreferTime.DAY, Instant.parse("2026-07-18T23:00:00Z"), seoul).nextPreferredTime())
                .isEqualTo(Instant.parse("2026-07-19T03:00:00Z"));
        assertThat(policy.evaluate(PreferTime.MORNING, Instant.parse("2026-07-19T04:00:00Z"), seoul).nextPreferredTime())
                .isEqualTo(Instant.parse("2026-07-19T21:00:00Z"));
    }

    @Test
    void anytimeIsAlwaysPreferred() {
        Instant now = Instant.parse("2026-07-19T04:00:00Z");
        var result = policy.evaluate(PreferTime.ANYTIME, now, seoul);
        assertThat(result.status()).isEqualTo(PREFERRED);
        assertThat(result.nextPreferredTime()).isEqualTo(now);
    }

    @Test
    void explicitSeoulZoneIgnoresSystemDefault() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
            assertStatus(PreferTime.DAY, "2026-07-19T04:00:00Z", PREFERRED);
        } finally { TimeZone.setDefault(original); }
    }

    private void assertStatus(PreferTime preferTime, String now, ProactiveSchedulingService.PreferredTimeStatus status) {
        assertThat(policy.evaluate(preferTime, Instant.parse(now), seoul).status()).isEqualTo(status);
    }
}
