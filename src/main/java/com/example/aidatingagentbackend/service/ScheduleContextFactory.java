package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.entity.CharacterSnapshotEntity;
import com.example.aidatingagentbackend.repository.CharacterSnapshotRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;

@Component
public class ScheduleContextFactory {
    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final CharacterSnapshotRepository repository;
    private final PreferredContactTimePolicy preferredContactTimePolicy;

    public ScheduleContextFactory(CharacterSnapshotRepository repository,
                                  PreferredContactTimePolicy preferredContactTimePolicy) {
        this.repository = repository;
        this.preferredContactTimePolicy = preferredContactTimePolicy;
    }

    public ProactiveSchedulingService.ScheduleContext fromLatestSnapshot(
            Long characterId, ProactiveSchedulingService.ScheduleContext base, Instant now) {
        return fromLatestSnapshot(characterId, base, now, DEFAULT_ZONE_ID);
    }

    public ProactiveSchedulingService.ScheduleContext fromLatestSnapshot(
            Long characterId, ProactiveSchedulingService.ScheduleContext base, Instant now, ZoneId zoneId) {
        CharacterSnapshotEntity snapshot = repository.findByCharacterId(characterId)
                .orElseThrow(() -> new IllegalArgumentException("character snapshot not found: " + characterId));
        var preferred = preferredContactTimePolicy.evaluate(snapshot.getPreferTime(), now, zoneId);
        return new ProactiveSchedulingService.ScheduleContext(now, base.lastContactAt(), base.firstInteractionAt(),
                preferred.nextPreferredTime(), base.proactiveContactEnabled(), base.explicitlyOptedOut(),
                base.doNotDisturb(), base.activeSession(), base.dailyContactCount(), base.dailyContactLimit(),
                preferred.status(), base.attachmentLevel(), base.relationshipState(), base.recentResponse(),
                base.consecutiveNoResponseCount(), base.busyLikely(), base.agentBusy(), base.callAllowed(),
                base.chatAllowed(), base.repeatedMissedCalls(), base.repairMessageAvailable());
    }
}
