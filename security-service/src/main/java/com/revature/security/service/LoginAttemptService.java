package com.revature.security.service;

import com.revature.security.dto.response.LoginHistoryResponse;
import com.revature.security.model.LoginAttempt;
import com.revature.security.repository.LoginAttemptRepository;
import com.revature.security.client.UserClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    private final LoginAttemptRepository loginAttemptRepository;
    private final UserClient userClient;
    private final SecurityAlertService securityAlertService;

    @Transactional
    public void recordLoginAttempt(String username, String email, boolean isSuccessful, String ipAddress,
            String userAgent) {
        try {
            Long userId = null;
            try {
                if (username != null) {
                    userId = userClient.getUserDetailsByUsername(username).getId();
                } else if (email != null) {
                    userId = userClient.getUserDetailsByEmail(email).getId();
                }
            } catch (Exception e) {
                logger.warn("User not found during login record: username={} email={}", username, email);
            }

            if (userId != null) {
                LoginAttempt attempt = LoginAttempt.builder()
                        .userId(userId)
                        .usernameAttempted(username)
                        .successful(isSuccessful)
                        .ipAddress(ipAddress)
                        .deviceInfo(userAgent)
                        .timestamp(LocalDateTime.now())
                        .build();

                loginAttemptRepository.save(attempt);

                if (!isSuccessful) {
                    checkFailedAttempts(userId, username);
                }
            } else {
                logger.warn("Cannot record login attempt for unknown user");
            }

        } catch (Exception e) {
            logger.error("Failed to record login attempt: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<LoginHistoryResponse> getLoginHistory(String username) {
        Long userId = userClient.getUserDetailsByUsername(username).getId();

        return loginAttemptRepository.findByUserIdOrderByTimestampDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void checkFailedAttempts(Long userId, String username) {
        LocalDateTime lockoutThreshold = LocalDateTime.now().minusMinutes(LOCKOUT_DURATION_MINUTES);
        long failedCount = loginAttemptRepository.countByUserIdAndIsSuccessfulFalseAndTimestampAfter(userId,
                lockoutThreshold);

        if (failedCount >= MAX_FAILED_ATTEMPTS) {
            logger.warn("Multiple failed login attempts detected for user {}: {}", username, failedCount);

            securityAlertService.createAlert(
                    username,
                    com.revature.security.model.SecurityAlert.AlertType.MULTIPLE_FAILED_LOGINS,
                    "Suspicious Login Activity",
                    "Multiple failed login attempts detected from your account.",
                    com.revature.security.model.SecurityAlert.Severity.HIGH);
        }
    }

    private LoginHistoryResponse mapToResponse(LoginAttempt attempt) {
        return LoginHistoryResponse.builder()
                .id(attempt.getId())
                .successful(attempt.isSuccessful())
                .ipAddress(attempt.getIpAddress())
                .deviceInfo(attempt.getDeviceInfo())
                .timestamp(attempt.getTimestamp())
                .build();
    }
}
