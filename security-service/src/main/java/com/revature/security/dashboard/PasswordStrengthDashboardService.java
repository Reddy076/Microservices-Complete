package com.revature.security.dashboard;

import com.revature.security.dto.response.PasswordAgeResponse;
import com.revature.security.dto.response.PasswordHealthMetricsResponse;
import com.revature.security.dto.response.ReusedPasswordResponse;
import com.revature.security.dto.response.SecurityScoreResponse;
import com.revature.security.dto.response.SecurityTrendResponse;

import com.revature.security.model.SecurityMetricsHistory;
import com.revature.security.model.AuditLog.AuditAction;
import com.revature.security.repository.SecurityMetricsHistoryRepository;
import com.revature.security.client.UserClient;
import com.revature.security.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PasswordStrengthDashboardService {

    private final SecurityMetricsCalculator metricsCalculator;
    private final SecurityMetricsHistoryRepository historyRepository;
    private final UserClient userClient;
    private final AuditLogService auditLogService;

    private static final long SNAPSHOT_COOLDOWN_MINUTES = 60;

    @Transactional
    public SecurityScoreResponse getSecurityScore(String username) {
        Long userId = userClient.getUserDetailsByUsername(username).getId();
        SecurityScoreResponse response = metricsCalculator.calculateSecurityScore(userId);
        persistSnapshotIfCooldownElapsed(userId, response);
        auditLogService.logAction(username, AuditAction.DASHBOARD_VIEWED, "Viewed security score dashboard");
        return response;
    }

    @Transactional
    public PasswordHealthMetricsResponse getPasswordHealth(String username) {
        Long userId = userClient.getUserDetailsByUsername(username).getId();
        auditLogService.logAction(username, AuditAction.DASHBOARD_VIEWED, "Viewed password health metrics");
        return metricsCalculator.calculatePasswordHealth(userId);
    }

    @Transactional
    public ReusedPasswordResponse getReusedPasswords(String username) {
        Long userId = userClient.getUserDetailsByUsername(username).getId();
        auditLogService.logAction(username, AuditAction.DASHBOARD_VIEWED, "Viewed reused passwords report");
        return metricsCalculator.findReusedPasswords(userId);
    }

    @Transactional
    public PasswordAgeResponse getPasswordAge(String username) {
        Long userId = userClient.getUserDetailsByUsername(username).getId();
        auditLogService.logAction(username, AuditAction.DASHBOARD_VIEWED, "Viewed password age distribution");
        return metricsCalculator.calculatePasswordAge(userId);
    }

    @Transactional(readOnly = true)
    public SecurityTrendResponse getSecurityTrends(String username, int days) {
        Long userId = userClient.getUserDetailsByUsername(username).getId();

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<SecurityMetricsHistory> history = historyRepository.findTrendData(userId, since);

        List<SecurityTrendResponse.TrendDataPoint> points = history.stream()
                .map(h -> SecurityTrendResponse.TrendDataPoint.builder()
                        .recordedAt(h.getRecordedAt())
                        .overallScore(h.getOverallScore())
                        .weakPasswordsCount(h.getWeakPasswordsCount())
                        .reusedPasswordsCount(h.getReusedPasswordsCount())
                        .oldPasswordsCount(h.getOldPasswordsCount())
                        .build())
                .collect(Collectors.toList());

        int scoreChange = computeScoreChange(points);
        String direction = scoreChange > 0 ? "IMPROVING" : scoreChange < 0 ? "DECLINING" : "STABLE";

        return SecurityTrendResponse.builder()
                .trendPoints(points)
                .scoreChange(scoreChange)
                .trendDirection(direction)
                .periodLabel(days + "-day trend")
                .build();
    }

    @Transactional(readOnly = true)
    public List<com.revature.security.dto.response.VaultEntryResponse> getWeakPasswordsList(String username) {
        Long userId = userClient.getUserDetailsByUsername(username).getId();
        return metricsCalculator.getWeakPasswordsList(userId);
    }

    @Transactional(readOnly = true)
    public List<com.revature.security.dto.response.VaultEntryResponse> getOldPasswordsList(String username) {
        Long userId = userClient.getUserDetailsByUsername(username).getId();
        return metricsCalculator.getOldPasswordsList(userId);
    }

    private void persistSnapshotIfCooldownElapsed(Long userId, SecurityScoreResponse score) {
        LocalDateTime cooldownThreshold = LocalDateTime.now().minusMinutes(SNAPSHOT_COOLDOWN_MINUTES);
        boolean recentSnapshotExists = historyRepository
                .findTopByUserIdOrderByRecordedAtDesc(userId)
                .map(latest -> latest.getRecordedAt().isAfter(cooldownThreshold))
                .orElse(false);

        if (!recentSnapshotExists) {
            SecurityMetricsHistory snapshot = SecurityMetricsHistory.builder()
                    .userId(userId)
                    .overallScore(score.getOverallScore())
                    .weakPasswordsCount(score.getWeakPasswords())
                    .reusedPasswordsCount(score.getReusedPasswords())
                    .oldPasswordsCount(score.getOldPasswords())
                    .strongPasswordsCount(score.getStrongPasswords())
                    .fairPasswordsCount(score.getFairPasswords())
                    .totalPasswordsCount(score.getTotalPasswords())
                    .build();
            historyRepository.save(snapshot);
        }
    }

    private int computeScoreChange(List<SecurityTrendResponse.TrendDataPoint> points) {
        if (points.size() < 2)
            return 0;
        int first = points.get(0).getOverallScore();
        int last = points.get(points.size() - 1).getOverallScore();
        return last - first;
    }
}
