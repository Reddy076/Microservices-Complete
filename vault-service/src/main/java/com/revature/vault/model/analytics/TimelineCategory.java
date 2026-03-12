package com.revature.vault.model.analytics;

/**
 * High-level grouping categories for timeline events.
 * Used by {@link ActivityType} and by the frontend for colour-coding.
 */
public enum TimelineCategory {
    VAULT,
    AUTH,
    BREACH,
    SHARING,
    BACKUP,
    SECURITY
}
