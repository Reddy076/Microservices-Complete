package com.revature.vault.model.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain model representing a single activity event on the vault timeline.
 *
 * <p>This is a transient (non-persisted) domain object that lives purely in
 * the service layer. It is produced by
 * {@link com.revature.vault.service.analytics.TimelineEventEnricher}
 * from raw {@link com.revature.vault.model.security.AuditLog} records
 * and then mapped to
 * {@link com.revature.vault.dto.response.TimelineEventDTO} before
 * being returned to the controller.</p>
 *
 * <p>Separating the domain model from the DTO allows the service layer to work
 * with typed {@link ActivityType} and {@link TimelinePeriod} values instead of
 * raw strings, while the DTO layer converts them to strings for JSON
 * serialisation.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineEvent {

    /** Audit log id from which this event was derived. */
    private Long sourceLogId;

    /** Strongly-typed activity type — never null. */
    private ActivityType activityType;

    /** Human-readable description of what happened. */
    private String description;

    /** Vault entry id referenced by this event, or {@code null} for session/system events. */
    private Long vaultEntryId;

    /** Vault entry title resolved at enrichment time, or {@code null}. */
    private String vaultEntryTitle;

    /** Website URL of the referenced vault entry, or {@code null}. */
    private String websiteUrl;

    /** IP address from which the action was performed, or {@code null}. */
    private String ipAddress;

    /** When the underlying audit log event occurred. */
    private LocalDateTime occurredAt;

    /**
     * Convenience accessor — delegates to {@link ActivityType#getCategory()}.
     *
     * @return the {@link TimelineCategory} for this event
     */
    public TimelineCategory getCategory() {
        return activityType != null ? activityType.getCategory() : null;
    }

    /**
     * Convenience accessor — returns the display label for this event type.
     *
     * @return human-readable event type label
     */
    public String getEventTypeLabel() {
        return activityType != null ? activityType.getDisplayLabel() : "Unknown";
    }
}
