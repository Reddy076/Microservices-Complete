package com.revature.security.controller;

import com.revature.security.model.AuditLog.AuditAction;
import com.revature.security.model.SecurityAlert.AlertType;
import com.revature.security.model.SecurityAlert.Severity;
import com.revature.security.service.AuditLogService;
import com.revature.security.service.SecurityAlertService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal endpoints called by user-service via its SecurityClient Feign interface.
 * Handles audit logging and security alerts triggered by authentication events
 * (login, 2FA enable/disable, password changes, etc.).
 *
 * Paths match user-service SecurityClient mappings:
 *   POST /internal/audit
 *   POST /internal/alerts
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalUserSecurityController {

    private static final Logger log = LoggerFactory.getLogger(InternalUserSecurityController.class);

    private final AuditLogService auditLogService;
    private final SecurityAlertService securityAlertService;

    /**
     * Called by user-service to log authentication audit events (login, logout, etc.).
     * POST /internal/audit?username=X&action=Y&details=Z&ipAddress=W
     */
    @PostMapping("/audit")
    public ResponseEntity<Void> logAudit(
            @RequestParam String username,
            @RequestParam String action,
            @RequestParam(required = false, defaultValue = "") String details,
            @RequestParam(required = false, defaultValue = "") String ipAddress) {
        try {
            AuditAction auditAction = AuditAction.valueOf(action);
            String fullDetails = ipAddress.isBlank() ? details : details + " (IP: " + ipAddress + ")";
            auditLogService.logAction(username, auditAction, fullDetails);
        } catch (IllegalArgumentException e) {
            // Unknown action — log as generic event
            log.warn("Unknown audit action '{}' from user-service for user {}", action, username);
            auditLogService.logAction(username, AuditAction.ACCOUNT_ACTIVITY, action + ": " + details);
        } catch (Exception e) {
            log.error("Failed to record audit for user {} action {}: {}", username, action, e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Called by user-service to create security alerts (2FA events, new device logins, etc.).
     * POST /internal/alerts?username=X&type=Y&title=Z&message=W&severity=V
     */
    @PostMapping("/alerts")
    public ResponseEntity<Void> createAlert(
            @RequestParam String username,
            @RequestParam String type,
            @RequestParam(required = false, defaultValue = "") String title,
            @RequestParam(required = false, defaultValue = "") String message,
            @RequestParam(required = false, defaultValue = "LOW") String severity) {
        try {
            // Log to audit trail
            AuditAction action;
            try {
                action = AuditAction.valueOf(type);
            } catch (IllegalArgumentException e) {
                action = AuditAction.ACCOUNT_ACTIVITY;
            }
            String details = title.isBlank() ? message : title + ": " + message;
            auditLogService.logAction(username, action, details);

            // Save as a real security alert in the DB
            AlertType alertType;
            try {
                alertType = AlertType.valueOf(type);
            } catch (IllegalArgumentException e) {
                log.warn("Unknown alert type '{}' — skipping alert creation for user {}", type, username);
                return ResponseEntity.ok().build();
            }
            Severity alertSeverity;
            try {
                alertSeverity = Severity.valueOf(severity.toUpperCase());
            } catch (IllegalArgumentException e) {
                alertSeverity = Severity.LOW;
            }
            securityAlertService.createAlert(username, alertType, title, message, alertSeverity);
            log.info("Security alert created for user {}: [{}] {} (severity={})", username, type, title, severity);
        } catch (Exception e) {
            log.error("Failed to record security alert for user {} type {}: {}", username, type, e.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}
