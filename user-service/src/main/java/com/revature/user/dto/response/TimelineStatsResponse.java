package com.revature.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Aggregated activity statistics for the vault timeline.
 * Provides data suited for charts: event counts by type, by day, by month.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineStatsResponse {

    /**
     * Count of events grouped by event type.
     * Key: event type string (e.g., "ENTRY_CREATED")
     * Value: count
     */
    private Map<String, Integer> eventsByType;

    /**
     * Count of events grouped by category.
     * Key: category string (e.g., "VAULT", "SECURITY")
     * Value: count
     */
    private Map<String, Integer> eventsByCategory;

    /**
     * Daily activity counts for the requested period.
     * Ordered chronologically.
     */
    private List<DailyActivityBucket> dailyActivity;

    /**
     * Monthly activity counts (last 12 months).
     * Ordered chronologically.
     */
    private List<MonthlyActivityBucket> monthlyActivity;

    /** Total events in the queried period */
    private int totalEvents;

    /** Average events per day in the queried period */
    private double averageEventsPerDay;

    /** Day with the highest activity in the queried period */
    private String peakActivityDate;

    /** Count on the peak activity day */
    private int peakActivityCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyActivityBucket {
        /** ISO date string (YYYY-MM-DD) */
        private String date;
        /** Total events on this day */
        private int count;
        /** Events broken down by category for stacked charts */
        private int vaultCount;
        private int securityCount;
        private int sharingCount;
        private int authCount;
        /** Fix: previously BACKUP events (VAULT_EXPORTED) were silently dropped; now tracked */
        private int backupCount;
        private int breachCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyActivityBucket {
        /** Year-month label (e.g., "2025-01") */
        private String month;
        /** Total events in this month */
        private int count;
    }
}
