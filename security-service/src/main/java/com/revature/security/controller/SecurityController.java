package com.revature.security.controller;

import com.revature.security.dto.SecurityAlertDTO;
import com.revature.security.dto.response.AuditLogResponse;
import com.revature.security.dto.response.LoginHistoryResponse;
import com.revature.security.dto.response.MessageResponse;
import com.revature.security.dto.response.SecurityAuditResponse;
import com.revature.security.service.AuditLogService;
import com.revature.security.service.LoginAttemptService;
import com.revature.security.service.SecurityAlertService;
import com.revature.security.service.SecurityAuditService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import com.revature.security.client.UserClient;

import java.util.List;

@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class SecurityController {
    private final UserClient userClient;

  private final AuditLogService auditLogService;
  private final LoginAttemptService loginAttemptService;
  private final SecurityAlertService securityAlertService;
  private final SecurityAuditService securityAuditService;

  @GetMapping("/audit-logs")
  public ResponseEntity<List<AuditLogResponse>> getAuditLogs(@RequestHeader("X-User-Name") String username) {
    
    return ResponseEntity.ok(auditLogService.getAuditLogs(username));
  }

  @GetMapping("/login-history")
  public ResponseEntity<List<LoginHistoryResponse>> getLoginHistory(@RequestHeader("X-User-Name") String username) {
    
    return ResponseEntity.ok(loginAttemptService.getLoginHistory(username));
  }

  @GetMapping("/alerts")
  public ResponseEntity<List<SecurityAlertDTO>> getAlerts(@RequestHeader("X-User-Name") String username) {
    
    return ResponseEntity.ok(securityAlertService.getAlerts(username));
  }

  @PutMapping("/alerts/{id}/read")
  public ResponseEntity<MessageResponse> markAlertAsRead(@RequestHeader("X-User-Name") String username, @PathVariable Long id) {
    
    securityAlertService.markAsRead(username, id);
    return ResponseEntity.ok(new MessageResponse("Alert marked as read"));
  }

  @DeleteMapping("/alerts/{id}")
  public ResponseEntity<MessageResponse> deleteAlert(@RequestHeader("X-User-Name") String username, @PathVariable Long id) {
    
    securityAlertService.deleteAlert(username, id);
    return ResponseEntity.ok(new MessageResponse("Alert deleted"));
  }

  @GetMapping("/audit-report")
  public ResponseEntity<SecurityAuditResponse> getAuditReport(@RequestHeader("X-User-Name") String username) {
    
    return ResponseEntity.ok(securityAuditService.generateAuditReport(username));
  }

  @GetMapping("/weak-passwords")
  public ResponseEntity<List<SecurityAuditResponse.VaultEntrySummary>> getWeakPasswords(@RequestHeader("X-User-Name") String username) {
    
    return ResponseEntity.ok(securityAuditService.getWeakPasswords(username));
  }

  @GetMapping("/reused-passwords")
  public ResponseEntity<List<SecurityAuditResponse.VaultEntrySummary>> getReusedPasswords(@RequestHeader("X-User-Name") String username) {
    
    return ResponseEntity.ok(securityAuditService.getReusedPasswords(username));
  }

  @GetMapping("/old-passwords")
  public ResponseEntity<List<SecurityAuditResponse.VaultEntrySummary>> getOldPasswords(@RequestHeader("X-User-Name") String username) {
    
    return ResponseEntity.ok(securityAuditService.getOldPasswords(username));
  }

  @PostMapping("/analyze-vault")
  public ResponseEntity<SecurityAuditResponse> analyzeVault(@RequestHeader("X-User-Name") String username) {
    

    return ResponseEntity.ok(securityAuditService.generateAuditReport(username));
  }

  
}







