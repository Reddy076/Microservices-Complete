package com.revature.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Feature 38 – Password Expiration Tracker.
 *
 * <p>Response for {@code GET /api/expiry/status} — full list of all vault entry expiry states.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpiryStatusResponse {

    /** Total number of active vault entries tracked. */
    private int totalEntries;

    /** Count of entries in FRESH state. */
    private int freshCount;

    /** Count of entries in AGING state. */
    private int agingCount;

    /** Count of entries in EXPIRING_SOON state. */
    private int expiringSoonCount;

    /** Count of entries in EXPIRED state. */
    private int expiredCount;

    /** Per-entry detail list. */
    private List<EntryExpiryDetail> entries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntryExpiryDetail {

        private Long entryId;
        private String title;
        private String username;
        private String websiteUrl;

        /** When the password was last changed. */
        private LocalDateTime lastChangedAt;

        /** Computed expiry timestamp. */
        private LocalDateTime expiresAt;

        /** Current expiry state. */
        private String status;

        /** Days remaining until expiry (negative = already expired). */
        private long daysUntilExpiry;

        /** Whether a reminder has been sent for this cycle. */
        private boolean reminderSent;

        /** Whether the reminder is currently snoozed. */
        private boolean snoozed;

        /** When the snooze expires (null if not snoozed). */
        private LocalDateTime snoozedUntil;
    }
}
