package com.revature.vault.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a single activity event on the vault timeline.
 * Events are derived from audit logs and vault entry metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineEventDTO {

    /** Unique identifier for this event (audit log id or synthetic id) */
    private Long id;

    /**
     * Type of activity event.
     * Values: ENTRY_CREATED, ENTRY_UPDATED, ENTRY_DELETED, ENTRY_RESTORED,
     *         PASSWORD_VIEWED, PASSWORD_CHANGED, SHARE_CREATED, SHARE_ACCESSED,
     *         SHARE_REVOKED, BREACH_DETECTED, BREACH_RESOLVED, VAULT_EXPORTED,
     *         LOGIN, LOGOUT
     */
    private String eventType;

    /**
     * Category group for color-coding on the frontend.
     * Values: VAULT, SECURITY, SHARING, AUTH, BREACH, BACKUP
     */
    private String category;

    /** Human-readable description of the event */
    private String description;

    /** Vault entry id if applicable (null for login/export/etc events) */
    private Long vaultEntryId;

    /** Title of the vault entry if applicable */
    private String vaultEntryTitle;

    /** Website URL of the vault entry if applicable */
    private String websiteUrl;

    /** IP address from which the action was performed */
    private String ipAddress;

    /** Timestamp of the event */
    private LocalDateTime timestamp;

    /**
     * Severity/importance level for the frontend to highlight critical events.
     * Values: LOW, MEDIUM, HIGH, CRITICAL
     */
    private String severity;
}
