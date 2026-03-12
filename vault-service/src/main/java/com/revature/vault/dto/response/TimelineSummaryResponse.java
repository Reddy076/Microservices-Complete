package com.revature.vault.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Summary statistics for the vault timeline.
 * Provides high-level counts and most-active entry data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineSummaryResponse {

    /** Total number of vault entries created all-time */
    private int totalEntriesCreated;

    /** Total number of passwords changed all-time (based on snapshots) */
    private int totalPasswordChanges;

    /** Total number of vault entries deleted all-time */
    private int totalEntriesDeleted;

    /** Total number of secure shares created */
    private int totalSharesCreated;

    /** Total number of breach detections */
    private int totalBreachDetections;

    /** Total number of audit log entries all-time */
    private int totalAuditEvents;

    /**
     * Most active day of the week by audit log count.
     * E.g., "Monday", "Friday"
     */
    private String mostActiveDayOfWeek;

    /**
     * Most active hour of the day (0-23) by audit log count.
     * Null when there are no audit log events yet (new user).
     */
    private Integer mostActiveHour;

    /** Top 5 most frequently accessed vault entries (by audit log count) */
    private List<EntryActivitySummary> mostAccessedEntries;

    /** Activity counts broken down by week for the last 12 weeks */
    private List<WeeklyActivityBucket> weeklyActivity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntryActivitySummary {
        private Long entryId;
        private String entryTitle;
        private String websiteUrl;
        private int accessCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyActivityBucket {
        /** ISO week start date (YYYY-MM-DD) */
        private String weekStart;
        /** Total number of events in this week */
        private int eventCount;
    }
}
