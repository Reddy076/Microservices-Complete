package com.revature.vault.service.analytics;

import com.revature.vault.client.SecurityClient;
import com.revature.vault.client.UserClient;
import com.revature.vault.dto.response.AuditLogResponse;
import com.revature.vault.dto.response.HeatmapResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Adapted from the monolith's AccessHeatmapService.
 * Fetches audit logs from security-service via Feign instead of a direct repository.
 */
@Service
@RequiredArgsConstructor
public class AccessHeatmapService {

    private final SecurityClient securityClient;
    private final UserClient userClient;

    @Transactional(readOnly = true)
    public HeatmapResponse getAccessHeatmap(String username) {
        List<AuditLogResponse> allLogs = securityClient.getAuditLogs(username);

        // Filter to last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<AuditLogResponse> logs = allLogs.stream()
                .filter(l -> l.getTimestamp() != null && l.getTimestamp().isAfter(thirtyDaysAgo))
                .toList();

        int[] hourlyCounts = new int[24];
        int[] dailyCounts  = new int[7];

        for (AuditLogResponse log : logs) {
            LocalDateTime ts = log.getTimestamp();
            hourlyCounts[ts.getHour()]++;
            int dayIndex = (ts.getDayOfWeek().getValue() % 7);
            dailyCounts[dayIndex]++;
        }

        List<Integer> hourlyList = new ArrayList<>();
        for (int c : hourlyCounts) hourlyList.add(c);

        List<Integer> dailyList = new ArrayList<>();
        for (int c : dailyCounts) dailyList.add(c);

        int peakHour = -1, maxHourCount = 0;
        for (int i = 0; i < 24; i++) {
            if (hourlyCounts[i] > maxHourCount) { maxHourCount = hourlyCounts[i]; peakHour = i; }
        }

        int peakDayIndex = -1, maxDayCount = 0;
        for (int i = 0; i < 7; i++) {
            if (dailyCounts[i] > maxDayCount) { maxDayCount = dailyCounts[i]; peakDayIndex = i; }
        }

        String peakDayName = "";
        if (peakDayIndex != -1) {
            int javaDayValue = (peakDayIndex == 0) ? 7 : peakDayIndex;
            peakDayName = DayOfWeek.of(javaDayValue).getDisplayName(TextStyle.FULL, Locale.getDefault());
        }

        return HeatmapResponse.builder()
                .accessByHour(hourlyList)
                .accessByDay(dailyList)
                .peakHour(peakHour)
                .peakDay(peakDayName)
                .totalAccesses(logs.size())
                .period("LAST_30_DAYS")
                .build();
    }
}
