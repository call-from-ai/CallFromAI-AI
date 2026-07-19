package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.CharacterSnapshot;
import com.example.aidatingagentbackend.dto.CharacterTraitSnapshot;
import com.example.aidatingagentbackend.entity.AgentLifeType;
import com.example.aidatingagentbackend.entity.CharacterSnapshotEntity;
import com.example.aidatingagentbackend.entity.PreferTime;
import com.example.aidatingagentbackend.repository.CharacterSnapshotRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static com.example.aidatingagentbackend.service.ProactiveSchedulingService.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScheduleContextFactoryTests {
    @Test
    void latestChangedSnapshotControlsSchedulingAndAnytimeDoesNotBlockCall() {
        CharacterSnapshotRepository repository = mock(CharacterSnapshotRepository.class);
        CharacterSnapshotEntity entity = entity(PreferTime.MORNING);
        when(repository.findByCharacterId(10L)).thenReturn(Optional.of(entity));
        ScheduleContextFactory factory = new ScheduleContextFactory(repository, new PreferredContactTimePolicy());
        ProactiveSchedulingService service = new ProactiveSchedulingService();
        Instant now = Instant.parse("2026-07-19T12:00:00Z"); // 21:00 Seoul

        ScheduleContext morning = factory.fromLatestSnapshot(10L, base(now), now, ZoneId.of("Asia/Seoul"));
        assertThat(service.decide(morning, .5).reason()).isEqualTo("NOT_PREFERRED_TIME");
        assertThat(service.decide(morning, .5).nextCheckAt()).isEqualTo(Instant.parse("2026-07-19T21:00:00Z"));

        entity.updateFrom(snapshot(PreferTime.LATE_EVENING));
        ScheduleContext evening = factory.fromLatestSnapshot(10L, base(now), now, ZoneId.of("Asia/Seoul"));
        assertThat(service.decide(evening, .5).action()).isEqualTo(ContactAction.CALL);

        entity.updateFrom(snapshot(PreferTime.ANYTIME));
        ScheduleContext anytime = factory.fromLatestSnapshot(10L, base(now), now, ZoneId.of("Asia/Seoul"));
        assertThat(service.decide(anytime, .5).action()).isEqualTo(ContactAction.CALL);
    }

    private ScheduleContext base(Instant now) {
        return new ScheduleContext(now, now.minusSeconds(36000), now.minusSeconds(60), null, true, false,
                false, false, 0, 3, PreferredTimeStatus.AMBIGUOUS, AttachmentLevel.NORMAL,
                RelationshipState.NORMAL, RecentResponse.POSITIVE, 0, false, false, true, true, false, false);
    }

    private CharacterSnapshotEntity entity(PreferTime preferTime) { return new CharacterSnapshotEntity(snapshot(preferTime)); }
    private CharacterSnapshot snapshot(PreferTime preferTime) {
        return new CharacterSnapshot(10L, "하나", null, null, null, AgentLifeType.WORKER, preferTime, 70,
                new CharacterTraitSnapshot(5,5,5,5,5,5,5,5,5,5,1));
    }
}
