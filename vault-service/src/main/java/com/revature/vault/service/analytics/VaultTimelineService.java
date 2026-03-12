package com.revature.vault.service.analytics;

import com.revature.vault.client.SecurityClient;
import com.revature.vault.client.UserClient;
import com.revature.vault.dto.response.*;
import com.revature.vault.exception.ResourceNotFoundException;
import com.revature.vault.model.analytics.TimelinePeriod;
import com.revature.vault.model.vault.VaultEntry;
import com.revature.vault.model.vault.VaultSnapshot;
import com.revature.vault.repository.SecureShareRepository;
import com.revature.vault.repository.VaultEntryRepository;
import com.revature.vault.repository.VaultSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service for Vault Timeline Visualization.
 * Adapted from the monolith's VaultTimelineService. Fetches audit logs from
 * security-service via Feign (SecurityClient) instead of direct repository access.
 */
@Service
@RequiredArgsConstructor
public class VaultTimelineService {

    private final SecurityClient securityClient;
    private final UserClient userClient;
    private final VaultEntryRepository vaultEntryRepository;
    private final VaultSnapshotRepository vaultSnapshotRepository;
    private final SecureShareRepository secureShareRepository;
    private final ActivityAggregator activityAggregator;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private static final Set<String> VALID_CATEGORIES =
            Set.of("VAULT", "AUTH", "BREACH", "SHARING", "BACKUP", "SECURITY");

    // ── Public API ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public VaultTimelineResponse getTimeline(String username, Integer days, String categoryFilter) {
        if (categoryFilter != null && !categoryFilter.isBlank()
                && !VALID_CATEGORIES.contains(categoryFilter.toUpperCase())) {
            throw new IllegalArgumentException(
                    "Invalid category filter '" + categoryFilter + "'. Valid values: " + VALID_CATEGORIES);
        }

        Long userId = userClient.getUserDetailsByUsername(username).getId();
        List<AuditLogResponse> logs = fetchLogs(username, days);

        // Log the timeline access via security-service
        securityClient.logAction(username, "TIMELINE_VIEWED", "Viewed vault timeline");

        List<TimelineEventDTO> events = activityAggregator.aggregate(logs, userId);

        if (categoryFilter != null && !categoryFilter.isBlank()) {
            final String upperCategory = categoryFilter.toUpperCase();
            events = events.stream()
                    .filter(e -> upperCategory.equalsIgnoreCase(e.getCategory()))
                    .collect(Collectors.toList());
        }

        VaultTimelineResponse.CategoryBreakdown breakdown = computeCategoryBreakdown(events);

        LocalDateTime earliest = logs.isEmpty() ? null
                : logs.get(logs.size() - 1).getTimestamp();
        TimelinePeriod period = TimelinePeriod.resolve(days, earliest);

        return VaultTimelineResponse.builder()
                .events(events)
                .totalEvents(events.size())
                .period(period.getLabel())
                .startDate(period.getStart().format(DATE_FORMATTER))
                .endDate(period.getEnd().format(DATE_FORMATTER))
                .categoryBreakdown(breakdown)
                .build();
    }

    @Transactional(readOnly = true)
    public TimelineSummaryResponse getSummary(String username) {
        Long userId = userClient.getUserDetailsByUsername(username).getId();
        securityClient.logAction(username, "TIMELINE_VIEWED", "Viewed timeline summary");

        List<AuditLogResponse> allLogs = securityClient.getAuditLogs(username);
        List<VaultSnapshot> snapshots = vaultSnapshotRepository.findByVaultEntryUserIdOrderByChangedAtDesc(userId);

        int totalCreated    = countByAction(allLogs, "ENTRY_CREATED");
        int totalDeleted    = countByAction(allLogs, "ENTRY_DELETED");
        int totalPasswordChanges = snapshots.size();
        int totalSharesCreated   = countByAction(allLogs, "SHARE_CREATED");
        int totalBreachDetections = countByAction(allLogs, "BREACH_DETECTED");
        int totalAuditEvents = allLogs.size();

        String mostActiveDayOfWeek = computeMostActiveDayOfWeek(allLogs);
        Integer mostActiveHour     = computeMostActiveHour(allLogs);

        List<TimelineSummaryResponse.EntryActivitySummary> mostAccessed =
                computeMostAccessedEntries(allLogs, userId);

        List<TimelineSummaryResponse.WeeklyActivityBucket> weeklyActivity =
                computeWeeklyActivity(allLogs);

        return TimelineSummaryResponse.builder()
                .totalEntriesCreated(totalCreated)
                .totalPasswordChanges(totalPasswordChanges)
                .totalEntriesDeleted(totalDeleted)
                .totalSharesCreated(totalSharesCreated)
                .totalBreachDetections(totalBreachDetections)
                .totalAuditEvents(totalAuditEvents)
                .mostActiveDayOfWeek(mostActiveDayOfWeek)
                .mostActiveHour(mostActiveHour)
                .mostAccessedEntries(mostAccessed)
                .weeklyActivity(weeklyActivity)
                .build();
    }

    @Transactional(readOnly = true)
    public EntryTimelineResponse getEntryTimeline(String username, Long entryId) {
        Long userId = userClient.getUserDetailsByUsername(username).getId();
        securityClient.logAction(username, "TIMELINE_VIEWED", "Viewed timeline for entry id=" + entryId);

        VaultEntry entry = vaultEntryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vault entry not found: " + entryId));

        List<AuditLogResponse> allUserLogs = securityClient.getAuditLogs(username);
        String entryTitle = entry.getTitle();
        List<AuditLogResponse> entryLogs = allUserLogs.stream()
                .filter(log -> isLogForEntry(log, entryId, entryTitle))
                .collect(Collectors.toList());

        List<TimelineEventDTO> events = activityAggregator.aggregate(entryLogs, userId);

        List<VaultSnapshot> snapshots = vaultSnapshotRepository.findByVaultEntryIdOrderByChangedAtDesc(entryId);
        int passwordChanges = snapshots.size();

        int passwordViews = countByAction(entryLogs, "PASSWORD_VIEWED");
        int shareCount = (int) secureShareRepository.findByOwnerIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(s -> s.getVaultEntry() != null && entryId.equals(s.getVaultEntry().getId()))
                .count();

        return EntryTimelineResponse.builder()
                .entryId(entryId)
                .entryTitle(entry.getTitle())
                .websiteUrl(entry.getWebsiteUrl())
                .deleted(Boolean.TRUE.equals(entry.getIsDeleted()))
                .events(events)
                .passwordChangeCount(passwordChanges)
                .passwordViewCount(passwordViews)
                .shareCount(shareCount)
                .build();
    }

    @Transactional(readOnly = true)
    public TimelineStatsResponse getStats(String username, Integer days) {
        Long userId = userClient.getUserDetailsByUsername(username).getId();
        securityClient.logAction(username, "TIMELINE_VIEWED", "Viewed timeline stats");

        List<AuditLogResponse> logs = fetchLogs(username, days);
        List<TimelineEventDTO> events = activityAggregator.aggregate(logs, userId);

        Map<String, Integer> byType     = new LinkedHashMap<>();
        Map<String, Integer> byCategory = new LinkedHashMap<>();

        for (TimelineEventDTO e : events) {
            byType.merge(e.getEventType(), 1, Integer::sum);
            byCategory.merge(e.getCategory(), 1, Integer::sum);
        }

        List<TimelineStatsResponse.DailyActivityBucket>   dailyActivity   = buildDailyBuckets(events);
        List<TimelineStatsResponse.MonthlyActivityBucket> monthlyActivity = buildMonthlyBuckets(events);

        int    total      = events.size();
        double avgPerDay  = computeAvgPerDay(events, days);
        String peakDate   = findPeakDate(dailyActivity);
        int    peakCount  = dailyActivity.stream()
                .mapToInt(TimelineStatsResponse.DailyActivityBucket::getCount)
                .max().orElse(0);

        return TimelineStatsResponse.builder()
                .eventsByType(byType)
                .eventsByCategory(byCategory)
                .dailyActivity(dailyActivity)
                .monthlyActivity(monthlyActivity)
                .totalEvents(total)
                .averageEventsPerDay(avgPerDay)
                .peakActivityDate(peakDate)
                .peakActivityCount(peakCount)
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<AuditLogResponse> fetchLogs(String username, Integer days) {
        List<AuditLogResponse> all = securityClient.getAuditLogs(username);
        if (days == null || days <= 0) return all;
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return all.stream()
                .filter(l -> l.getTimestamp() != null && l.getTimestamp().isAfter(since))
                .collect(Collectors.toList());
    }

    private int countByAction(List<AuditLogResponse> logs, String action) {
        return (int) logs.stream().filter(l -> action.equals(l.getAction())).count();
    }

    private VaultTimelineResponse.CategoryBreakdown computeCategoryBreakdown(List<TimelineEventDTO> events) {
        int vault = 0, security = 0, sharing = 0, auth = 0, breach = 0, backup = 0;
        for (TimelineEventDTO e : events) {
            switch (e.getCategory()) {
                case "VAULT"    -> vault++;
                case "SECURITY" -> security++;
                case "SHARING"  -> sharing++;
                case "AUTH"     -> auth++;
                case "BREACH"   -> breach++;
                case "BACKUP"   -> backup++;
            }
        }
        return VaultTimelineResponse.CategoryBreakdown.builder()
                .vaultEvents(vault)
                .securityEvents(security)
                .sharingEvents(sharing)
                .authEvents(auth)
                .breachEvents(breach)
                .backupEvents(backup)
                .build();
    }

    private String computeMostActiveDayOfWeek(List<AuditLogResponse> logs) {
        if (logs.isEmpty()) return null;
        int[] counts = new int[7];
        for (AuditLogResponse log : logs) {
            if (log.getTimestamp() == null) continue;
            int dayIdx = log.getTimestamp().getDayOfWeek().getValue() % 7;
            counts[dayIdx]++;
        }
        int maxIdx = 0;
        for (int i = 1; i < 7; i++) {
            if (counts[i] > counts[maxIdx]) maxIdx = i;
        }
        int javaDayVal = maxIdx == 0 ? 7 : maxIdx;
        return DayOfWeek.of(javaDayVal).getDisplayName(TextStyle.FULL, Locale.getDefault());
    }

    private Integer computeMostActiveHour(List<AuditLogResponse> logs) {
        if (logs.isEmpty()) return null;
        int[] counts = new int[24];
        for (AuditLogResponse log : logs) {
            if (log.getTimestamp() != null)
                counts[log.getTimestamp().getHour()]++;
        }
        int maxHour = 0;
        for (int i = 1; i < 24; i++) {
            if (counts[i] > counts[maxHour]) maxHour = i;
        }
        return maxHour;
    }

    private List<TimelineSummaryResponse.EntryActivitySummary> computeMostAccessedEntries(
            List<AuditLogResponse> allLogs, Long userId) {

        Map<String, Integer> titleAccessCounts = new LinkedHashMap<>();
        for (AuditLogResponse log : allLogs) {
            if ("PASSWORD_VIEWED".equals(log.getAction()) || "ENTRY_UPDATED".equals(log.getAction())) {
                String title = activityAggregator.extractEntryTitle(log.getDetails(), log.getAction());
                if (title != null) {
                    titleAccessCounts.merge(title, 1, Integer::sum);
                }
            }
        }

        Map<String, VaultEntry> titleCache = activityAggregator.buildTitleCache(userId);

        return titleAccessCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    VaultEntry entry = titleCache.get(e.getKey().toLowerCase());
                    return TimelineSummaryResponse.EntryActivitySummary.builder()
                            .entryId(entry != null ? entry.getId() : null)
                            .entryTitle(e.getKey())
                            .websiteUrl(entry != null ? entry.getWebsiteUrl() : null)
                            .accessCount(e.getValue())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<TimelineSummaryResponse.WeeklyActivityBucket> computeWeeklyActivity(List<AuditLogResponse> allLogs) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWindow = today.minusWeeks(12);

        Map<LocalDate, Integer> weekBuckets = new LinkedHashMap<>();
        for (int w = 11; w >= 0; w--) {
            LocalDate weekStart = today.minusWeeks(w).with(DayOfWeek.MONDAY);
            weekBuckets.put(weekStart, 0);
        }

        for (AuditLogResponse log : allLogs) {
            if (log.getTimestamp() == null) continue;
            LocalDate logDate = log.getTimestamp().toLocalDate();
            if (!logDate.isBefore(startOfWindow)) {
                LocalDate weekStart = logDate.with(DayOfWeek.MONDAY);
                weekBuckets.merge(weekStart, 1, Integer::sum);
            }
        }

        return weekBuckets.entrySet().stream()
                .map(e -> TimelineSummaryResponse.WeeklyActivityBucket.builder()
                        .weekStart(e.getKey().format(DATE_FORMATTER))
                        .eventCount(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private boolean isLogForEntry(AuditLogResponse log, Long entryId, String entryTitle) {
        String action = log.getAction();
        if (action == null) return false;
        if (!Set.of("ENTRY_CREATED", "ENTRY_UPDATED", "ENTRY_DELETED", "ENTRY_RESTORED",
                "PASSWORD_VIEWED", "SHARE_CREATED", "SHARE_REVOKED",
                "BREACH_DETECTED", "BREACH_RESOLVED").contains(action)) {
            return false;
        }
        String extractedTitle = activityAggregator.extractEntryTitle(log.getDetails(), action);
        return extractedTitle != null && extractedTitle.equalsIgnoreCase(entryTitle);
    }

    private List<TimelineStatsResponse.DailyActivityBucket> buildDailyBuckets(List<TimelineEventDTO> events) {
        Map<LocalDate, TimelineStatsResponse.DailyActivityBucket> buckets = new TreeMap<>();
        for (TimelineEventDTO event : events) {
            LocalDate date = event.getTimestamp().toLocalDate();
            buckets.computeIfAbsent(date, d -> TimelineStatsResponse.DailyActivityBucket.builder()
                    .date(d.format(DATE_FORMATTER))
                    .count(0).vaultCount(0).securityCount(0)
                    .sharingCount(0).authCount(0).backupCount(0).breachCount(0)
                    .build());
            TimelineStatsResponse.DailyActivityBucket bucket = buckets.get(date);
            bucket.setCount(bucket.getCount() + 1);
            switch (event.getCategory()) {
                case "VAULT"    -> bucket.setVaultCount(bucket.getVaultCount() + 1);
                case "SECURITY" -> bucket.setSecurityCount(bucket.getSecurityCount() + 1);
                case "BREACH"   -> bucket.setBreachCount(bucket.getBreachCount() + 1);
                case "SHARING"  -> bucket.setSharingCount(bucket.getSharingCount() + 1);
                case "AUTH"     -> bucket.setAuthCount(bucket.getAuthCount() + 1);
                case "BACKUP"   -> bucket.setBackupCount(bucket.getBackupCount() + 1);
            }
        }
        return new ArrayList<>(buckets.values());
    }

    private List<TimelineStatsResponse.MonthlyActivityBucket> buildMonthlyBuckets(List<TimelineEventDTO> events) {
        Map<String, Integer> buckets = new TreeMap<>();
        for (TimelineEventDTO event : events) {
            String month = event.getTimestamp().format(MONTH_FORMATTER);
            buckets.merge(month, 1, Integer::sum);
        }
        return buckets.entrySet().stream()
                .map(e -> TimelineStatsResponse.MonthlyActivityBucket.builder()
                        .month(e.getKey())
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private double computeAvgPerDay(List<TimelineEventDTO> events, Integer days) {
        if (events.isEmpty()) return 0.0;
        long effectiveDays;
        if (days != null && days > 0) {
            effectiveDays = days;
        } else {
            LocalDateTime oldest = events.stream()
                    .map(TimelineEventDTO::getTimestamp)
                    .min(Comparator.naturalOrder())
                    .orElse(LocalDateTime.now());
            effectiveDays = java.time.temporal.ChronoUnit.DAYS.between(oldest, LocalDateTime.now()) + 1;
            if (effectiveDays <= 0) effectiveDays = 1;
        }
        return Math.round((double) events.size() / effectiveDays * 100.0) / 100.0;
    }

    private String findPeakDate(List<TimelineStatsResponse.DailyActivityBucket> dailyBuckets) {
        return dailyBuckets.stream()
                .max(Comparator.comparingInt(TimelineStatsResponse.DailyActivityBucket::getCount))
                .map(TimelineStatsResponse.DailyActivityBucket::getDate)
                .orElse(null);
    }
}
