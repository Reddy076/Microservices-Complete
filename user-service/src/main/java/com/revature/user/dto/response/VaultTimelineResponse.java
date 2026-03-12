package com.revature.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Full paginated vault timeline response.
 * Contains a list of timeline events ordered by timestamp descending.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaultTimelineResponse {

    /** Ordered list of timeline events (most recent first) */
    private List<TimelineEventDTO> events;

    /** Total number of events in the filtered result set */
    private int totalEvents;

    /** Time range label for the current query (e.g., "LAST_30_DAYS", "ALL_TIME") */
    private String period;

    /** Start of the time range (ISO datetime string) */
    private String startDate;

    /** End of the time range (ISO datetime string) */
    private String endDate;

    /** Breakdown of event counts by category */
    private CategoryBreakdown categoryBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryBreakdown {
        private int vaultEvents;
        private int securityEvents;
        private int sharingEvents;
        private int authEvents;
        private int breachEvents;
        private int backupEvents;
    }
}
