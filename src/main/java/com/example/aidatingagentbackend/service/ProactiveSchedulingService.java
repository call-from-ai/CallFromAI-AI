package com.example.aidatingagentbackend.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Deprecated(forRemoval = true)
public class ProactiveSchedulingService {

    private static final Duration MINIMUM_COOLDOWN = Duration.ofMinutes(30);
    private static final Duration FIRST_CONTACT_WINDOW = Duration.ofHours(1);

    public ScheduleDecision decide(ScheduleContext context) {
        return decide(context, ThreadLocalRandom.current().nextDouble());
    }

    ScheduleDecision decide(ScheduleContext context, double randomValue) {
        if (context == null) {
            return ScheduleDecision.blocked("MISSING_CONTEXT");
        }

        String hardFilterReason = hardFilterReason(context);
        if (hardFilterReason != null) {
            return ScheduleDecision.blocked(hardFilterReason);
        }

        if (context.lastContactAt() != null && context.consecutiveNoResponseCount() >= 3) {
            return ScheduleDecision.blocked("THREE_NO_RESPONSES");
        }
        if (context.lastContactAt() != null && context.consecutiveNoResponseCount() == 2) {
            return ScheduleDecision.defer("TWO_NO_RESPONSES", context.nextPreferredTime(), null);
        }

        if (context.preferredTimeStatus() == PreferredTimeStatus.NOT_PREFERRED) {
            return ScheduleDecision.defer("NOT_PREFERRED_TIME", context.nextPreferredTime(), null);
        }

        if (context.lastContactAt() == null) {
            return decideFirstContact(context);
        }

        Duration interval = contactInterval(context, randomValue);
        Instant dueAt = context.lastContactAt().plus(interval);
        if (context.now().isBefore(dueAt)) {
            return ScheduleDecision.defer("INTERVAL_NOT_PASSED", dueAt, interval);
        }

        if (context.busyLikely() || context.agentBusy()) {
            return ScheduleDecision.defer("BUSY", null, interval);
        }

        if (canCall(context)) {
            return new ScheduleDecision(ContactAction.CALL, "CALL_CONDITIONS_MET", context.now(), interval);
        }
        if (canChat(context)) {
            return new ScheduleDecision(ContactAction.CHAT, "CHAT_CONDITIONS_MET", context.now(), interval);
        }
        return ScheduleDecision.defer("NO_AVAILABLE_CHANNEL", null, interval);
    }

    private ScheduleDecision decideFirstContact(ScheduleContext context) {
        if (context.firstInteractionAt() == null) {
            return ScheduleDecision.blocked("MISSING_FIRST_INTERACTION_TIME");
        }
        if (context.now().isBefore(context.firstInteractionAt())) {
            return ScheduleDecision.defer("FIRST_CONTACT_NOT_STARTED", context.firstInteractionAt(), null);
        }
        Instant windowEndsAt = context.firstInteractionAt().plus(FIRST_CONTACT_WINDOW);
        if (context.now().isAfter(windowEndsAt)) {
            return ScheduleDecision.blocked("FIRST_CONTACT_WINDOW_EXPIRED");
        }
        if (!context.chatAllowed()) {
            return ScheduleDecision.defer("FIRST_CONTACT_CHAT_UNAVAILABLE", null, null);
        }
        return new ScheduleDecision(ContactAction.CHAT, "FIRST_CONTACT", context.now(), null);
    }

    private String hardFilterReason(ScheduleContext context) {
        if (!context.proactiveContactEnabled()) return "PROACTIVE_CONTACT_DISABLED";
        if (context.explicitlyOptedOut()) return "EXPLICIT_OPT_OUT";
        if (context.doNotDisturb()) return "DO_NOT_DISTURB";
        if (context.activeSession()) return "ACTIVE_SESSION";
        if (context.dailyContactCount() >= context.dailyContactLimit()) return "DAILY_LIMIT_REACHED";
        if (context.now() == null) return "MISSING_CONTACT_TIME";
        if (context.lastContactAt() != null
                && context.now().isBefore(context.lastContactAt().plus(MINIMUM_COOLDOWN))) {
            return "MINIMUM_COOLDOWN";
        }
        return null;
    }

    private Duration contactInterval(ScheduleContext context, double randomValue) {
        if (context.relationshipState() == RelationshipState.CONFLICT) {
            return conflictInterval(context, randomValue);
        }
        return randomizedInterval(adjustedInterval(context), randomValue);
    }

    private Duration conflictInterval(ScheduleContext context, double randomValue) {
        Duration minimum = switch (context.attachmentLevel()) {
            case LOW -> Duration.ofHours(5);
            case NORMAL -> Duration.ofHours(4);
            case HIGH -> Duration.ofHours(3);
        };
        double responseFloor = switch (context.recentResponse()) {
            case POSITIVE -> 0.0;
            case AMBIGUOUS -> 1.0 / 3.0;
            case NO_RESPONSE -> 2.0 / 3.0;
        };
        double bounded = Math.max(0.0, Math.min(1.0, randomValue));
        double adjustedRandomValue = responseFloor + bounded * (1.0 - responseFloor);
        return randomBetween(minimum, minimum.plusHours(1), adjustedRandomValue);
    }

    private Duration adjustedInterval(ScheduleContext context) {
        Duration base = switch (context.attachmentLevel()) {
            case LOW -> Duration.ofHours(3);
            case NORMAL -> Duration.ofHours(2);
            case HIGH -> Duration.ofMinutes(90);
        };

        Duration relationshipAdjustment = switch (context.relationshipState()) {
            case NORMAL -> Duration.ZERO;
            case UPSET, REPAIRING -> Duration.ofHours(1);
            case CONFLICT -> throw new IllegalStateException("Conflict interval must use its policy range");
        };
        Duration responseAdjustment = switch (context.recentResponse()) {
            case POSITIVE -> Duration.ZERO;
            case AMBIGUOUS -> Duration.ofMinutes(30);
            case NO_RESPONSE -> Duration.ofMinutes(45L * Math.max(1, context.consecutiveNoResponseCount()));
        };
        return base.plus(relationshipAdjustment).plus(responseAdjustment);
    }

    private Duration randomizedInterval(Duration interval, double randomValue) {
        double bounded = Math.max(0.0, Math.min(1.0, randomValue));
        // -1/6 ~ +1/4: two hours becomes the policy range of 1h40m ~ 2h30m.
        double multiplier = (5.0 / 6.0) + bounded * (5.0 / 12.0);
        return Duration.ofSeconds(Math.round(interval.toSeconds() * multiplier));
    }

    private Duration randomBetween(Duration minimum, Duration maximum, double randomValue) {
        double bounded = Math.max(0.0, Math.min(1.0, randomValue));
        long rangeSeconds = maximum.minus(minimum).toSeconds();
        return minimum.plusSeconds(Math.round(rangeSeconds * bounded));
    }

    private boolean canCall(ScheduleContext context) {
        return context.callAllowed()
                && context.preferredTimeStatus() == PreferredTimeStatus.PREFERRED
                && context.recentResponse() == RecentResponse.POSITIVE
                && context.relationshipState() == RelationshipState.NORMAL
                && !context.repeatedMissedCalls();
    }

    private boolean canChat(ScheduleContext context) {
        if (!context.chatAllowed()) {
            return false;
        }
        return context.relationshipState() != RelationshipState.CONFLICT || context.repairMessageAvailable();
    }

    public enum ContactAction { BLOCKED, DEFER, CHAT, CALL }
    public enum AttachmentLevel { LOW, NORMAL, HIGH }
    public enum RelationshipState { NORMAL, UPSET, CONFLICT, REPAIRING }
    public enum PreferredTimeStatus { PREFERRED, AMBIGUOUS, NOT_PREFERRED }
    public enum RecentResponse { POSITIVE, AMBIGUOUS, NO_RESPONSE }

    public record ScheduleContext(
            Instant now,
            Instant lastContactAt,
            Instant firstInteractionAt,
            Instant nextPreferredTime,
            boolean proactiveContactEnabled,
            boolean explicitlyOptedOut,
            boolean doNotDisturb,
            boolean activeSession,
            int dailyContactCount,
            int dailyContactLimit,
            PreferredTimeStatus preferredTimeStatus,
            AttachmentLevel attachmentLevel,
            RelationshipState relationshipState,
            RecentResponse recentResponse,
            int consecutiveNoResponseCount,
            boolean busyLikely,
            boolean agentBusy,
            boolean callAllowed,
            boolean chatAllowed,
            boolean repeatedMissedCalls,
            boolean repairMessageAvailable
    ) {
        public ScheduleContext {
            preferredTimeStatus = preferredTimeStatus == null ? PreferredTimeStatus.AMBIGUOUS : preferredTimeStatus;
            attachmentLevel = attachmentLevel == null ? AttachmentLevel.NORMAL : attachmentLevel;
            relationshipState = relationshipState == null ? RelationshipState.NORMAL : relationshipState;
            recentResponse = recentResponse == null ? RecentResponse.AMBIGUOUS : recentResponse;
            dailyContactCount = Math.max(0, dailyContactCount);
            dailyContactLimit = Math.max(0, dailyContactLimit);
            consecutiveNoResponseCount = Math.max(0, consecutiveNoResponseCount);
        }
    }

    public record ScheduleDecision(ContactAction action, String reason, Instant nextCheckAt, Duration interval) {
        static ScheduleDecision blocked(String reason) {
            return new ScheduleDecision(ContactAction.BLOCKED, reason, null, null);
        }

        static ScheduleDecision defer(String reason, Instant nextCheckAt, Duration interval) {
            return new ScheduleDecision(ContactAction.DEFER, reason, nextCheckAt, interval);
        }
    }
}
