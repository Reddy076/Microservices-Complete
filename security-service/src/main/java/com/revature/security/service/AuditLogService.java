package com.revature.security.service;

import com.revature.security.dto.response.AuditLogResponse;
import com.revature.security.model.AuditLog;
import com.revature.security.model.AuditLog.AuditAction;
import com.revature.security.repository.AuditLogRepository;
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
public class AuditLogService {

  private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

  private final AuditLogRepository auditLogRepository;
  private final UserClient userClient;

  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  public void logAction(String username, AuditAction action, String details) {
    logAction(username, action, details, null);
  }

  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  public void logAction(String username, AuditAction action, String details, String ipAddress) {
    try {
      Long userId = userClient.getUserDetailsByUsername(username).getId();
      if (userId == null) {
        logger.warn("Cannot log audit action for unknown user: {}", username);
        return;
      }

      AuditLog log = AuditLog.builder()
          .userId(userId)
          .action(action)
          .details(details)
          .ipAddress(ipAddress)
          .timestamp(LocalDateTime.now())
          .build();

      auditLogRepository.save(log);
      logger.debug("Audit log: {} - {} - {}", username, action, details);
    } catch (Exception e) {

      logger.error("Failed to create audit log: {}", e.getMessage());
    }
  }

  @Transactional(readOnly = true)
  public List<AuditLogResponse> getAuditLogs(String username) {
    Long userId = userClient.getUserDetailsByUsername(username).getId();

    List<AuditLog> logs = auditLogRepository.findByUserIdOrderByTimestampDesc(userId);
    return logs.stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  private AuditLogResponse mapToResponse(AuditLog log) {
    return AuditLogResponse.builder()
        .id(log.getId())
        .action(log.getAction().name())
        .details(log.getDetails())
        .ipAddress(log.getIpAddress())
        .timestamp(log.getTimestamp())
        .build();
  }
}






