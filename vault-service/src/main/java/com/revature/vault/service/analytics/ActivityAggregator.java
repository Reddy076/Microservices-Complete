package com.revature.vault.service.analytics;

import com.revature.vault.dto.response.AuditLogResponse;
import com.revature.vault.dto.response.TimelineEventDTO;
import com.revature.vault.model.analytics.ActivityType;
import com.revature.vault.model.analytics.TimelineEvent;
import com.revature.vault.model.vault.VaultEntry;
import com.revature.vault.repository.VaultEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pipeline orchestrator for the Vault Timeline feature.
 * Adapted from the monolith's ActivityAggregator with package changes only.
 * Uses AuditLogResponse DTOs (received via Feign from security-service) instead of entity objects.
 */
@Component
@RequiredArgsConstructor
public class ActivityAggregator {

    private final VaultEntryRepository vaultEntryRepository;
    private final TimelineEventEnricher enricher;

    // ── Public API ────────────────────────────────────────────────────────────

    public List<TimelineEventDTO> aggregate(List<AuditLogResponse> logs, Long userId) {
        Map<String, VaultEntry> titleCache = buildTitleCache(userId);
        List<TimelineEvent> events = enricher.enrichAll(logs, titleCache);
        return enricher.toDtoList(events);
    }

    public TimelineEventDTO mapLogToEvent(AuditLogResponse log, Map<String, VaultEntry> titleCache) {
        TimelineEvent event = enricher.enrich(log, titleCache);
        return enricher.toDto(event);
    }

    // ── Delegation helpers (retained for backward-compat with VaultTimelineService) ──

    public String extractEntryTitle(String details, String action) {
        return enricher.extractEntryTitle(details, action);
    }

    public String resolveCategory(String action) {
        ActivityType type = enricher.resolveActivityType(action);
        return type != null ? type.getCategory().name() : "SECURITY";
    }

    public String resolveSeverity(String action) {
        ActivityType type = enricher.resolveActivityType(action);
        return enricher.resolveSeverity(type);
    }

    // ── Title cache ───────────────────────────────────────────────────────────

    public Map<String, VaultEntry> buildTitleCache(Long userId) {
        List<VaultEntry> all = vaultEntryRepository.findByUserId(userId);
        Map<String, VaultEntry> cache = new HashMap<>();
        for (VaultEntry e : all) {
            if (e.getTitle() == null) continue;
            String key = e.getTitle().toLowerCase();
            VaultEntry existing = cache.get(key);
            if (existing == null ||
                    (Boolean.TRUE.equals(existing.getIsDeleted()) && !Boolean.TRUE.equals(e.getIsDeleted()))) {
                cache.put(key, e);
            }
        }
        return cache;
    }
}
