package com.revature.vault.service.analytics;

import com.revature.vault.dto.response.TimelineEventDTO;
import com.revature.vault.model.analytics.ActivityType;
import com.revature.vault.model.analytics.TimelineEvent;
import com.revature.vault.model.vault.VaultEntry;
import com.revature.vault.dto.response.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enriches raw AuditLogResponse records into fully-typed TimelineEvent
 * domain objects, and maps those domain objects to TimelineEventDTOs for API responses.
 * Adapted from the monolith's TimelineEventEnricher with package changes only.
 */
@Component
@RequiredArgsConstructor
public class TimelineEventEnricher {

    // ── Public API ────────────────────────────────────────────────────────────

    public TimelineEvent enrich(AuditLogResponse log, Map<String, VaultEntry> titleCache) {
        String actionStr = log.getAction();
        ActivityType activityType = resolveActivityType(actionStr);

        String extractedTitle = extractEntryTitle(log.getDetails(), actionStr);
        Long vaultEntryId = null;
        String vaultEntryTitle = null;
        String websiteUrl = null;

        if (extractedTitle != null) {
            vaultEntryTitle = extractedTitle;
            VaultEntry entry = titleCache.get(extractedTitle.toLowerCase());
            if (entry != null) {
                vaultEntryId = entry.getId();
                websiteUrl = entry.getWebsiteUrl();
            }
        }

        return TimelineEvent.builder()
                .sourceLogId(log.getId())
                .activityType(activityType)
                .description(buildDescription(log, actionStr))
                .vaultEntryId(vaultEntryId)
                .vaultEntryTitle(vaultEntryTitle)
                .websiteUrl(websiteUrl)
                .ipAddress(log.getIpAddress())
                .occurredAt(log.getTimestamp())
                .build();
    }

    public List<TimelineEvent> enrichAll(List<AuditLogResponse> logs, Map<String, VaultEntry> titleCache) {
        return logs.stream()
                .map(log -> enrich(log, titleCache))
                .collect(Collectors.toList());
    }

    public TimelineEventDTO toDto(TimelineEvent event) {
        return TimelineEventDTO.builder()
                .id(event.getSourceLogId())
                .eventType(event.getActivityType() != null
                        ? event.getActivityType().name() : "UNKNOWN")
                .category(event.getCategory() != null
                        ? event.getCategory().name() : "SECURITY")
                .description(event.getDescription())
                .vaultEntryId(event.getVaultEntryId())
                .vaultEntryTitle(event.getVaultEntryTitle())
                .websiteUrl(event.getWebsiteUrl())
                .ipAddress(event.getIpAddress())
                .timestamp(event.getOccurredAt())
                .severity(resolveSeverity(event.getActivityType()))
                .build();
    }

    public List<TimelineEventDTO> toDtoList(List<TimelineEvent> events) {
        return events.stream().map(this::toDto).collect(Collectors.toList());
    }

    // ── ActivityType resolution ───────────────────────────────────────────────

    public ActivityType resolveActivityType(String action) {
        if (action == null) return null;
        return ActivityType.fromAuditAction(action);
    }

    // ── Severity resolution ───────────────────────────────────────────────────

    public String resolveSeverity(ActivityType type) {
        if (type == null) return "LOW";
        return switch (type) {
            case BREACH_DETECTED, LOGIN_FAILED -> "CRITICAL";
            case ENTRY_DELETED, SHARE_CREATED, BREACH_SCAN_RUN -> "HIGH";
            case ENTRY_UPDATED, ENTRY_RESTORED, SHARE_ACCESSED, SHARE_REVOKED,
                 VAULT_EXPORTED, BREACH_RESOLVED -> "MEDIUM";
            default -> "LOW";
        };
    }

    // ── Entry title extraction ────────────────────────────────────────────────

    public String extractEntryTitle(String details, String action) {
        if (details == null || details.isBlank() || action == null) return null;
        return switch (action) {
            case "ENTRY_CREATED" -> extractAfterPrefix(details, "Created entry: ");
            case "ENTRY_UPDATED" -> {
                String t = extractAfterPrefix(details, "Updated entry: ");
                if (t == null) t = extractAfterPrefix(details, "Toggled sensitive flag for entry: ");
                yield t;
            }
            case "ENTRY_DELETED" -> extractAfterPrefix(details, "Deleted entry: ");
            case "ENTRY_RESTORED" -> extractAfterPrefix(details, "Restored entry: ");
            case "PASSWORD_VIEWED" -> extractAfterPrefix(details, "Viewed password for entry: ");
            case "SHARE_CREATED" -> extractQuotedTitle(details, "Shared entry '");
            case "SHARE_REVOKED" -> extractQuotedTitle(details, "entry='");
            default -> null;
        };
    }

    // ── Description builder ───────────────────────────────────────────────────

    private String buildDescription(AuditLogResponse log, String action) {
        String details = log.getDetails();
        return switch (action != null ? action : "") {
            case "LOGIN" -> "Successful login"
                    + (log.getIpAddress() != null ? " from " + log.getIpAddress() : "");
            case "LOGIN_FAILED" -> "Failed login attempt"
                    + (log.getIpAddress() != null ? " from " + log.getIpAddress() : "");
            case "LOGOUT" -> "Session ended";
            case "BREACH_SCAN_RUN" -> "Breach scan executed against HaveIBeenPwned";
            case "DASHBOARD_VIEWED" -> "Security dashboard accessed";
            case "TIMELINE_VIEWED" -> "Vault timeline accessed";
            default -> details != null
                    ? details : (action != null ? action.replace('_', ' ').toLowerCase() : "");
        };
    }

    // ── String helpers ────────────────────────────────────────────────────────

    private String extractAfterPrefix(String details, String prefix) {
        if (!details.startsWith(prefix)) return null;
        String rest = details.substring(prefix.length()).trim();
        for (String stop : new String[]{"' token=", " token=", " (", "\n"}) {
            int idx = rest.indexOf(stop);
            if (idx > 0) rest = rest.substring(0, idx);
        }
        return rest.isBlank() ? null : rest.trim();
    }

    private String extractQuotedTitle(String details, String prefix) {
        int start = details.indexOf(prefix);
        if (start < 0) return null;
        int titleStart = start + prefix.length();
        int titleEnd = details.indexOf("'", titleStart);
        if (titleEnd < 0) return null;
        String title = details.substring(titleStart, titleEnd).trim();
        return title.isBlank() ? null : title;
    }
}
