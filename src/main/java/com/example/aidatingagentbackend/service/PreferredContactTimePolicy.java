package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.entity.PreferTime;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static com.example.aidatingagentbackend.service.ProactiveSchedulingService.PreferredTimeStatus;

@Component
@Deprecated(forRemoval = true)
public class PreferredContactTimePolicy {
    static final LocalTime MORNING_START = LocalTime.of(6, 0);
    static final LocalTime DAY_START = LocalTime.of(12, 0);
    static final LocalTime LATE_EVENING_START = LocalTime.of(18, 0);

    public Result evaluate(PreferTime preferTime, Instant now, ZoneId zoneId) {
        if (now == null) throw new IllegalArgumentException("now is required");
        if (zoneId == null) throw new IllegalArgumentException("zoneId is required");
        PreferTime effective = preferTime == null ? PreferTime.ANYTIME : preferTime;
        if (effective == PreferTime.ANYTIME) {
            return new Result(PreferredTimeStatus.PREFERRED, now);
        }

        ZonedDateTime localNow = now.atZone(zoneId);
        LocalTime time = localNow.toLocalTime();
        LocalTime start = startOf(effective);
        LocalTime end = endOf(effective);
        if (!time.isBefore(start) && (effective == PreferTime.LATE_EVENING || time.isBefore(end))) {
            return new Result(PreferredTimeStatus.PREFERRED, now);
        }

        LocalDate nextDate = time.isBefore(start) ? localNow.toLocalDate() : localNow.toLocalDate().plusDays(1);
        return new Result(PreferredTimeStatus.NOT_PREFERRED,
                ZonedDateTime.of(nextDate, start, zoneId).toInstant());
    }

    private LocalTime startOf(PreferTime preferTime) {
        return switch (preferTime) {
            case MORNING -> MORNING_START;
            case DAY -> DAY_START;
            case LATE_EVENING -> LATE_EVENING_START;
            case ANYTIME -> throw new IllegalArgumentException("ANYTIME has no start");
        };
    }

    private LocalTime endOf(PreferTime preferTime) {
        return switch (preferTime) {
            case MORNING -> DAY_START;
            case DAY -> LATE_EVENING_START;
            case LATE_EVENING -> LocalTime.MAX;
            case ANYTIME -> throw new IllegalArgumentException("ANYTIME has no end");
        };
    }

    public record Result(PreferredTimeStatus status, Instant nextPreferredTime) {}
}
