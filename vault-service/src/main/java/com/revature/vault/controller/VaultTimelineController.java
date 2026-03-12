package com.revature.vault.controller;

import com.revature.vault.dto.response.*;
import com.revature.vault.service.analytics.AccessHeatmapService;
import com.revature.vault.service.analytics.VaultTimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Vault Timeline Visualization.
 * Adapted from the monolith's VaultTimelineController — package changes only.
 * Base URL: /api/vault/timeline  (routed by API Gateway under /api/vault/**)
 */
@RestController
@RequestMapping("/api/vault/timeline")
@RequiredArgsConstructor
@Tag(name = "Vault Timeline", description = "Vault Timeline Visualization: chronological activity feed and analytics")
public class VaultTimelineController {

    private final VaultTimelineService timelineService;
    private final AccessHeatmapService heatmapService;

    @Operation(summary = "Get full vault timeline",
               description = "Returns the complete chronological activity timeline for the authenticated user. " +
                             "Optionally filter by time range (?days=N) and/or event category (?category=VAULT).")
    @GetMapping
    public ResponseEntity<VaultTimelineResponse> getTimeline(
            @Parameter(description = "Number of past days to include (omit for all-time)")
            @RequestParam(required = false) Integer days,
            @Parameter(description = "Filter by category: VAULT, AUTH, BREACH, SHARING, BACKUP, SECURITY")
            @RequestParam(name = "category", required = false) String categoryFilter,
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(timelineService.getTimeline(username, days, categoryFilter));
    }

    @Operation(summary = "Get timeline activity summary",
               description = "Returns high-level summary statistics: total events by type, most active day/hour, " +
                             "top accessed entries, and a 12-week weekly activity histogram.")
    @GetMapping("/summary")
    public ResponseEntity<TimelineSummaryResponse> getSummary(
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(timelineService.getSummary(username));
    }

    @Operation(summary = "Get timeline for a specific vault entry",
               description = "Returns the full lifecycle timeline for a single vault entry. Entry must belong to the current user.")
    @GetMapping("/entry/{entryId}")
    public ResponseEntity<EntryTimelineResponse> getEntryTimeline(
            @Parameter(description = "Vault entry id")
            @PathVariable Long entryId,
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(timelineService.getEntryTimeline(username, entryId));
    }

    @Operation(summary = "Get timeline activity statistics",
               description = "Returns chart-ready aggregated statistics: daily activity, monthly totals, event counts by type and category.")
    @GetMapping("/stats")
    public ResponseEntity<TimelineStatsResponse> getStats(
            @Parameter(description = "Number of past days to include in stats (omit for all-time)")
            @RequestParam(required = false) Integer days,
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(timelineService.getStats(username, days));
    }

    @Operation(summary = "Get access heatmap for last 30 days",
               description = "Returns hourly and daily activity counts for the past 30 days for heatmap chart rendering.")
    @GetMapping("/heatmap")
    public ResponseEntity<HeatmapResponse> getHeatmap(
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(heatmapService.getAccessHeatmap(username));
    }
}
