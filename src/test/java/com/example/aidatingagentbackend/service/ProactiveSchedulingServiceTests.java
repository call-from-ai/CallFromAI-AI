package com.example.aidatingagentbackend.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static com.example.aidatingagentbackend.service.ProactiveSchedulingService.*;
import static org.assertj.core.api.Assertions.assertThat;

class ProactiveSchedulingServiceTests {

    private final ProactiveSchedulingService service = new ProactiveSchedulingService();
    private final Instant now = Instant.parse("2026-07-17T12:00:00Z");

    @Test
    void hardFilterStopsEvaluationImmediately() {
        ScheduleDecision result = service.decide(context().doNotDisturb(true).build(), 0.5);
        assertDecision(result, ContactAction.BLOCKED, "DO_NOT_DISTURB");
    }

    @Test
    void normalAttachmentUsesPolicyRandomRange() {
        assertRange(AttachmentLevel.NORMAL, Duration.ofMinutes(100), Duration.ofMinutes(150));
    }

    @Test
    void lowAndHighAttachmentUseRandomRanges() {
        assertRange(AttachmentLevel.LOW, Duration.ofMinutes(150), Duration.ofMinutes(225));
        assertRange(AttachmentLevel.HIGH, Duration.ofMinutes(75), Duration.ofMinutes(112).plusSeconds(30));
    }

    @Test
    void dailyLimitReachedIsBlocked() {
        assertDecision(service.decide(context().dailyContactCount(3).dailyContactLimit(3).build(), 0.5),
                ContactAction.BLOCKED, "DAILY_LIMIT_REACHED");
    }

    @Test
    void zeroDailyLimitIsBlocked() {
        assertDecision(service.decide(context().dailyContactLimit(0).build(), 0.5),
                ContactAction.BLOCKED, "DAILY_LIMIT_REACHED");
    }

    @Test
    void explicitOptOutIsBlocked() {
        assertDecision(service.decide(context().explicitlyOptedOut(true).build(), 0.5),
                ContactAction.BLOCKED, "EXPLICIT_OPT_OUT");
    }

    @Test
    void disabledProactiveContactIsBlocked() {
        assertDecision(service.decide(context().proactiveContactEnabled(false).build(), 0.5),
                ContactAction.BLOCKED, "PROACTIVE_CONTACT_DISABLED");
    }

    @Test
    void activeSessionIsBlocked() {
        assertDecision(service.decide(context().activeSession(true).build(), 0.5),
                ContactAction.BLOCKED, "ACTIVE_SESSION");
    }

    @Test
    void nonPreferredTimeDefersToNextPreferredTime() {
        ScheduleDecision result = service.decide(context()
                .preferredTimeStatus(PreferredTimeStatus.NOT_PREFERRED).build(), 0.5);

        assertDecision(result, ContactAction.DEFER, "NOT_PREFERRED_TIME");
        assertThat(result.nextCheckAt()).isEqualTo(now.plus(Duration.ofHours(8)));
    }

    @Test
    void oneNoResponseOnlyIncreasesInterval() {
        ScheduleDecision result = service.decide(context()
                .recentResponse(RecentResponse.NO_RESPONSE)
                .consecutiveNoResponseCount(1)
                .lastContactAt(now.minus(Duration.ofHours(10)))
                .build(), 0.0);

        assertDecision(result, ContactAction.CHAT, "CHAT_CONDITIONS_MET");
        assertThat(result.interval()).isEqualTo(Duration.ofMinutes(137).plusSeconds(30));
    }

    @Test
    void twoNoResponsesDeferBeforeIntervalCalculation() {
        ScheduleDecision result = service.decide(context()
                .consecutiveNoResponseCount(2)
                .lastContactAt(now.minus(Duration.ofMinutes(31)))
                .build(), 0.5);

        assertDecision(result, ContactAction.DEFER, "TWO_NO_RESPONSES");
        assertThat(result.nextCheckAt()).isEqualTo(now.plus(Duration.ofHours(8)));
        assertThat(result.interval()).isNull();
    }

    @Test
    void threeNoResponsesBlockBeforeIntervalCalculation() {
        ScheduleDecision result = service.decide(context()
                .consecutiveNoResponseCount(3)
                .lastContactAt(now.minus(Duration.ofMinutes(31)))
                .build(), 0.5);

        assertDecision(result, ContactAction.BLOCKED, "THREE_NO_RESPONSES");
        assertThat(result.interval()).isNull();
    }

    @Test
    void upsetAndRepairingApplyOneHourAdjustmentWithoutAbnormalSpread() {
        for (RelationshipState state : new RelationshipState[]{RelationshipState.UPSET, RelationshipState.REPAIRING}) {
            ScheduleDecision minimum = service.decide(context().relationshipState(state)
                    .recentResponse(RecentResponse.POSITIVE).build(), 0.0);
            ScheduleDecision maximum = service.decide(context().relationshipState(state)
                    .recentResponse(RecentResponse.POSITIVE).build(), 1.0);

            assertThat(minimum.interval()).isEqualTo(Duration.ofMinutes(150));
            assertThat(maximum.interval()).isEqualTo(Duration.ofMinutes(225));
        }
    }

    @Test
    void busyUserDefers() {
        ScheduleDecision result = service.decide(context().busyLikely(true)
                .lastContactAt(now.minus(Duration.ofHours(10))).build(), 0.5);
        assertDecision(result, ContactAction.DEFER, "BUSY");
    }

    @Test
    void firstContactWithinOneHourUsesChat() {
        ScheduleDecision result = service.decide(context().lastContactAt(null)
                .firstInteractionAt(now.minus(Duration.ofMinutes(30))).callAllowed(true).build(), 0.5);

        assertDecision(result, ContactAction.CHAT, "FIRST_CONTACT");
        assertThat(result.interval()).isNull();
    }

    @Test
    void firstContactStillHonorsHardFilters() {
        ScheduleDecision result = service.decide(context().lastContactAt(null)
                .firstInteractionAt(now.minus(Duration.ofMinutes(30))).doNotDisturb(true).build(), 0.5);
        assertDecision(result, ContactAction.BLOCKED, "DO_NOT_DISTURB");
    }

    @Test
    void firstContactStillHonorsPreferredTime() {
        ScheduleDecision result = service.decide(context().lastContactAt(null)
                .firstInteractionAt(now.minus(Duration.ofMinutes(30)))
                .preferredTimeStatus(PreferredTimeStatus.NOT_PREFERRED).build(), 0.5);

        assertDecision(result, ContactAction.DEFER, "NOT_PREFERRED_TIME");
        assertThat(result.nextCheckAt()).isEqualTo(now.plus(Duration.ofHours(8)));
    }

    @Test
    void expiredFirstContactWindowDoesNotSend() {
        ScheduleDecision result = service.decide(context().lastContactAt(null)
                .firstInteractionAt(now.minus(Duration.ofHours(2))).build(), 0.5);
        assertDecision(result, ContactAction.BLOCKED, "FIRST_CONTACT_WINDOW_EXPIRED");
    }

    @Test
    void conflictIntervalsStayInsidePersonalityPolicyRanges() {
        assertConflictRange(AttachmentLevel.LOW, Duration.ofHours(5), Duration.ofHours(6));
        assertConflictRange(AttachmentLevel.NORMAL, Duration.ofHours(4), Duration.ofHours(5));
        assertConflictRange(AttachmentLevel.HIGH, Duration.ofHours(3), Duration.ofHours(4));
    }

    @Test
    void conflictResponseAdjustmentStaysInsideFinalPolicyRange() {
        ScheduleDecision positive = service.decide(context().relationshipState(RelationshipState.CONFLICT)
                .recentResponse(RecentResponse.POSITIVE).build(), 0.0);
        ScheduleDecision noResponse = service.decide(context().relationshipState(RelationshipState.CONFLICT)
                .recentResponse(RecentResponse.NO_RESPONSE).consecutiveNoResponseCount(1).build(), 0.0);

        assertThat(positive.interval()).isEqualTo(Duration.ofHours(4));
        assertThat(noResponse.interval()).isBetween(Duration.ofHours(4), Duration.ofHours(5));
        assertThat(noResponse.interval()).isGreaterThan(positive.interval());
    }

    @Test
    void conflictOnlyAllowsRepairChat() {
        ScheduleDecision deferred = service.decide(context().relationshipState(RelationshipState.CONFLICT)
                .lastContactAt(now.minus(Duration.ofHours(10))).build(), 0.4);
        ScheduleDecision repair = service.decide(context().relationshipState(RelationshipState.CONFLICT)
                .repairMessageAvailable(true).lastContactAt(now.minus(Duration.ofHours(10))).build(), 0.4);

        assertDecision(deferred, ContactAction.DEFER, "NO_AVAILABLE_CHANNEL");
        assertDecision(repair, ContactAction.CHAT, "CHAT_CONDITIONS_MET");
    }

    @Test
    void callRequiresEveryStrictCallCondition() {
        ScheduleDecision call = service.decide(context().preferredTimeStatus(PreferredTimeStatus.PREFERRED)
                .recentResponse(RecentResponse.POSITIVE).callAllowed(true).build(), 0.5);
        ScheduleDecision chat = service.decide(context().preferredTimeStatus(PreferredTimeStatus.PREFERRED)
                .recentResponse(RecentResponse.POSITIVE).callAllowed(true).repeatedMissedCalls(true).build(), 0.5);

        assertThat(call.action()).isEqualTo(ContactAction.CALL);
        assertThat(chat.action()).isEqualTo(ContactAction.CHAT);
    }

    private void assertRange(AttachmentLevel level, Duration minimum, Duration maximum) {
        ScheduleDecision minResult = service.decide(context().attachmentLevel(level)
                .recentResponse(RecentResponse.POSITIVE).lastContactAt(now.minus(Duration.ofMinutes(31))).build(), 0.0);
        ScheduleDecision maxResult = service.decide(context().attachmentLevel(level)
                .recentResponse(RecentResponse.POSITIVE).lastContactAt(now.minus(Duration.ofMinutes(31))).build(), 1.0);
        assertThat(minResult.interval()).isEqualTo(minimum);
        assertThat(maxResult.interval()).isEqualTo(maximum);
    }

    private void assertConflictRange(AttachmentLevel level, Duration minimum, Duration maximum) {
        ScheduleDecision minResult = service.decide(context().attachmentLevel(level)
                .relationshipState(RelationshipState.CONFLICT)
                .recentResponse(RecentResponse.POSITIVE).build(), 0.0);
        ScheduleDecision maxResult = service.decide(context().attachmentLevel(level)
                .relationshipState(RelationshipState.CONFLICT)
                .recentResponse(RecentResponse.POSITIVE).build(), 1.0);
        assertThat(minResult.interval()).isEqualTo(minimum);
        assertThat(maxResult.interval()).isEqualTo(maximum);
    }

    private void assertDecision(ScheduleDecision result, ContactAction action, String reason) {
        assertThat(result.action()).isEqualTo(action);
        assertThat(result.reason()).isEqualTo(reason);
    }

    private ContextBuilder context() { return new ContextBuilder(); }

    private final class ContextBuilder {
        private Instant lastContactAt = now.minus(Duration.ofHours(10));
        private Instant firstInteractionAt = now.minus(Duration.ofMinutes(30));
        private boolean proactiveContactEnabled = true;
        private boolean explicitlyOptedOut;
        private boolean doNotDisturb;
        private boolean activeSession;
        private int dailyContactCount;
        private int dailyContactLimit = 3;
        private PreferredTimeStatus preferredTimeStatus = PreferredTimeStatus.AMBIGUOUS;
        private AttachmentLevel attachmentLevel = AttachmentLevel.NORMAL;
        private RelationshipState relationshipState = RelationshipState.NORMAL;
        private RecentResponse recentResponse = RecentResponse.AMBIGUOUS;
        private int consecutiveNoResponseCount;
        private boolean busyLikely;
        private boolean callAllowed;
        private boolean repeatedMissedCalls;
        private boolean repairMessageAvailable;

        ContextBuilder lastContactAt(Instant value) { lastContactAt = value; return this; }
        ContextBuilder firstInteractionAt(Instant value) { firstInteractionAt = value; return this; }
        ContextBuilder proactiveContactEnabled(boolean value) { proactiveContactEnabled = value; return this; }
        ContextBuilder explicitlyOptedOut(boolean value) { explicitlyOptedOut = value; return this; }
        ContextBuilder doNotDisturb(boolean value) { doNotDisturb = value; return this; }
        ContextBuilder activeSession(boolean value) { activeSession = value; return this; }
        ContextBuilder dailyContactCount(int value) { dailyContactCount = value; return this; }
        ContextBuilder dailyContactLimit(int value) { dailyContactLimit = value; return this; }
        ContextBuilder preferredTimeStatus(PreferredTimeStatus value) { preferredTimeStatus = value; return this; }
        ContextBuilder attachmentLevel(AttachmentLevel value) { attachmentLevel = value; return this; }
        ContextBuilder relationshipState(RelationshipState value) { relationshipState = value; return this; }
        ContextBuilder recentResponse(RecentResponse value) { recentResponse = value; return this; }
        ContextBuilder consecutiveNoResponseCount(int value) { consecutiveNoResponseCount = value; return this; }
        ContextBuilder busyLikely(boolean value) { busyLikely = value; return this; }
        ContextBuilder callAllowed(boolean value) { callAllowed = value; return this; }
        ContextBuilder repeatedMissedCalls(boolean value) { repeatedMissedCalls = value; return this; }
        ContextBuilder repairMessageAvailable(boolean value) { repairMessageAvailable = value; return this; }

        ScheduleContext build() {
            return new ScheduleContext(now, lastContactAt, firstInteractionAt, now.plus(Duration.ofHours(8)),
                    proactiveContactEnabled, explicitlyOptedOut, doNotDisturb, activeSession,
                    dailyContactCount, dailyContactLimit, preferredTimeStatus, attachmentLevel,
                    relationshipState, recentResponse, consecutiveNoResponseCount, busyLikely, false,
                    callAllowed, true, repeatedMissedCalls, repairMessageAvailable);
        }
    }
}
