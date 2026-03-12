package com.revature.vault.model.analytics;

/**
 * Canonical activity type enum for the Feature 37 Vault Timeline.
 *
 * <p>Maps raw {@link com.revature.vault.model.security.AuditLog.AuditAction}
 * values to named, timeline-specific activity types. This decouples the timeline domain
 * model from the raw audit action strings and gives a single place to define the full
 * set of observable vault activities.</p>
 *
 * <p>Each constant carries its display label and the high-level
 * {@link TimelineCategory} it belongs to, enabling the frontend to group and
 * colour-code events without coupling to string literals.</p>
 */
public enum ActivityType {

    // ── Vault entry lifecycle ─────────────────────────────────────────────────
    ENTRY_CREATED("Entry Created",         TimelineCategory.VAULT),
    ENTRY_UPDATED("Entry Updated",         TimelineCategory.VAULT),
    ENTRY_DELETED("Entry Deleted",         TimelineCategory.VAULT),
    ENTRY_RESTORED("Entry Restored",       TimelineCategory.VAULT),
    PASSWORD_VIEWED("Password Viewed",     TimelineCategory.VAULT),

    // ── Authentication ────────────────────────────────────────────────────────
    LOGIN("Login",                         TimelineCategory.AUTH),
    LOGIN_FAILED("Failed Login Attempt",   TimelineCategory.AUTH),
    LOGOUT("Logout",                       TimelineCategory.AUTH),

    // ── Breach monitoring ─────────────────────────────────────────────────────
    BREACH_SCAN_RUN("Breach Scan Run",     TimelineCategory.BREACH),
    BREACH_DETECTED("Breach Detected",     TimelineCategory.BREACH),
    BREACH_RESOLVED("Breach Resolved",     TimelineCategory.BREACH),

    // ── Secure sharing ────────────────────────────────────────────────────────
    SHARE_CREATED("Share Created",         TimelineCategory.SHARING),
    SHARE_ACCESSED("Share Accessed",       TimelineCategory.SHARING),
    SHARE_REVOKED("Share Revoked",         TimelineCategory.SHARING),

    // ── Backup / export ───────────────────────────────────────────────────────
    VAULT_EXPORTED("Vault Exported",       TimelineCategory.BACKUP),

    // ── Analytics / system ───────────────────────────────────────────────────
    DASHBOARD_VIEWED("Dashboard Viewed",   TimelineCategory.SECURITY),
    TIMELINE_VIEWED("Timeline Viewed",     TimelineCategory.SECURITY);

    // ── Fields ────────────────────────────────────────────────────────────────

    private final String displayLabel;
    private final TimelineCategory category;

    ActivityType(String displayLabel, TimelineCategory category) {
        this.displayLabel = displayLabel;
        this.category = category;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public TimelineCategory getCategory() {
        return category;
    }

    /**
     * Safely maps an {@link com.revature.vault.model.security.AuditLog.AuditAction}
     * name string to an {@link ActivityType}, returning {@code null} if the action
     * has no corresponding timeline activity type.
     *
     * @param auditActionName the {@code AuditAction.name()} string
     * @return the matching {@code ActivityType}, or {@code null}
     */
    public static ActivityType fromAuditAction(String auditActionName) {
        if (auditActionName == null) return null;
        try {
            return ActivityType.valueOf(auditActionName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
