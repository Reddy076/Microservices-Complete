package com.revature.security.service;

import com.revature.security.dto.SecurityAlertDTO;

import com.revature.security.model.SecurityAlert;
import com.revature.security.model.SecurityAlert.AlertType;
import com.revature.security.model.SecurityAlert.Severity;
import com.revature.security.repository.SecurityAlertRepository;
import com.revature.security.client.UserClient;
import com.revature.security.client.NotificationClient;
import com.revature.security.client.NotificationClient.NotificationRequest;
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
public class SecurityAlertService {

  private static final Logger logger = LoggerFactory.getLogger(SecurityAlertService.class);

  private final SecurityAlertRepository securityAlertRepository;
  private final UserClient userClient;
  private final NotificationClient notificationClient;

  @Transactional
  public void createAlert(String username, AlertType alertType, String title,
      String message, Severity severity) {
    try {
      Long userId = userClient.getUserDetailsByUsername(username).getId();
      if (userId == null) {
        logger.warn("Cannot create alert for unknown user: {}", username);
        return;
      }

      SecurityAlert alert = SecurityAlert.builder()
          .userId(userId)
          .alertType(alertType)
          .title(title)
          .message(message)
          .severity(severity)
          .isRead(false)
          .createdAt(LocalDateTime.now())
          .build();

      securityAlertRepository.save(alert);

      notificationClient.sendNotification(NotificationRequest.builder().username(username).type("SECURITY_ALERT").title("Security Alert: " + title).message(message).build());

      logger.debug("Security alert created: {} - {} for user {}", alertType, title, username);
    } catch (Exception e) {
      logger.error("Failed to create security alert: {}", e.getMessage());
    }
  }

  @Transactional(readOnly = true)
  public List<SecurityAlertDTO> getAlerts(String username) {
    Long userId = userClient.getUserDetailsByUsername(username).getId();

    return securityAlertRepository.findByUserIdOrderByCreatedAtDesc(userId)
        .stream()
        .map(this::mapToDTO)
        .collect(Collectors.toList());
  }

  @Transactional
  public void markAsRead(String username, Long alertId) {
    Long userId = userClient.getUserDetailsByUsername(username).getId();

    SecurityAlert alert = securityAlertRepository.findById(alertId)
        .orElseThrow(() -> new RuntimeException("Alert not found"));

    if (!alert.getUserId().equals(userId)) {
      throw new IllegalArgumentException("Alert does not belong to user");
    }

    alert.setRead(true);
    securityAlertRepository.save(alert);
  }

  @Transactional
  public void deleteAlert(String username, Long alertId) {
    Long userId = userClient.getUserDetailsByUsername(username).getId();

    SecurityAlert alert = securityAlertRepository.findById(alertId)
        .orElseThrow(() -> new RuntimeException("Alert not found"));

    if (!alert.getUserId().equals(userId)) {
      throw new IllegalArgumentException("Alert does not belong to user");
    }

    securityAlertRepository.delete(alert);
  }

  private SecurityAlertDTO mapToDTO(SecurityAlert alert) {
    return SecurityAlertDTO.builder()
        .id(alert.getId())
        .alertType(alert.getAlertType().name())
        .title(alert.getTitle())
        .message(alert.getMessage())
        .severity(alert.getSeverity().name())
        .isRead(alert.isRead())
        .createdAt(alert.getCreatedAt())
        .build();
  }
}










