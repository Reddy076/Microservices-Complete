package com.revature.user.service.security;

import com.revature.user.model.security.LoginAttempt;
import com.revature.user.client.SecurityClient;
import com.revature.user.repository.LoginAttemptRepository;
import com.revature.user.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdaptiveAuthService {

  private static final Logger logger = LoggerFactory.getLogger(AdaptiveAuthService.class);

  private final LoginAttemptRepository loginAttemptRepository;
  private final SecurityClient securityClient;
  private final DateTimeUtil dateTimeUtil;

  public int calculateRiskScore(String username, String ipAddress, String deviceInfo, String deviceFingerprint) {
    int riskScore = 0;

    List<LoginAttempt> recentAttempts = loginAttemptRepository
        .findByUsernameAttemptedOrderByTimestampDesc(username);

    if (recentAttempts.isEmpty()) {
      riskScore += 30;
    } else {
      boolean knownIp = recentAttempts.stream()
          .anyMatch(a -> a.getIpAddress() != null && a.getIpAddress().equals(ipAddress));
      if (!knownIp) {
        riskScore += 40;
        securityClient.createAlert(username,
            "NEW_LOCATION_LOGIN",
            "Login from New IP",
            "Login detected from IP: " + ipAddress,
            "MEDIUM");
      }

      boolean knownFingerprint = recentAttempts.stream()
          .anyMatch(a -> a.getDeviceInfo() != null && a.getDeviceInfo().contains(deviceFingerprint));
      if (!knownFingerprint) {
        riskScore += 25;

      }

      long recentFailures = recentAttempts.stream()
          .filter(a -> !a.isSuccessful() && a.getTimestamp().isAfter(LocalDateTime.now().minusHours(1)))
          .count();
      if (recentFailures >= 3) {
        riskScore += 30;
      }

      if (dateTimeUtil.isUnusualTime(LocalDateTime.now())) {
        riskScore += 20;
      }
    }

    return Math.min(100, riskScore);
  }

  public boolean requiresAdditionalVerification(int riskScore) {
    return riskScore >= 50;
  }
}
