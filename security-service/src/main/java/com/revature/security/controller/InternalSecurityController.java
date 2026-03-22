package com.revature.security.controller;

import com.revature.security.model.AuditLog.AuditAction;
import com.revature.security.model.PasswordAnalysis;
import com.revature.security.repository.PasswordAnalysisRepository;
import com.revature.security.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal endpoints called by vault-service via SecurityClient Feign.
 * Handles audit logging and password analysis triggered by vault operations.
 */
@RestController
@RequestMapping("/api/security/internal")
@RequiredArgsConstructor
public class InternalSecurityController {

    private static final Logger log = LoggerFactory.getLogger(InternalSecurityController.class);

    private final AuditLogService auditLogService;
    private final PasswordAnalysisRepository passwordAnalysisRepository;

    /**
     * Called by vault-service when vault entries are created/updated/deleted/viewed.
     * Example: POST /api/security/internal/audit?username=Reddy&action=ENTRY_CREATED&details=...
     */
    @PostMapping("/audit")
    public ResponseEntity<Void> logAction(
            @RequestParam String username,
            @RequestParam String action,
            @RequestParam(required = false, defaultValue = "") String details) {
        try {
            AuditAction auditAction = AuditAction.valueOf(action);
            auditLogService.logAction(username, auditAction, details);
        } catch (IllegalArgumentException e) {
            // Unknown action string — log as warning but don't fail
            log.warn("Unknown audit action '{}' from vault-service for user {}", action, username);
        } catch (Exception e) {
            log.error("Failed to record audit log for user {} action {}: {}", username, action, e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Called by vault-service after creating/updating a vault entry to analyze password strength.
     */
    @PostMapping("/analyze")
    public ResponseEntity<Void> analyzePasswordEntry(@RequestBody VaultEntryPayload payload) {
        try {
            if (payload == null || payload.getId() == null) {
                return ResponseEntity.ok().build();
            }

            int score = computeStrengthScore(payload.getPassword());
            List<String> issues = computeIssues(payload.getPassword());

            PasswordAnalysis existing = passwordAnalysisRepository.findByVaultEntryId(payload.getId()).orElse(null);

            if (existing != null) {
                existing.setStrengthScore(score);
                existing.setIssues(issues);
                existing.setLastAnalyzed(LocalDateTime.now());
                passwordAnalysisRepository.save(existing);
            } else {
                PasswordAnalysis analysis = PasswordAnalysis.builder()
                        .vaultEntryId(payload.getId())
                        .strengthScore(score)
                        .isReused(false)
                        .issues(issues)
                        .lastAnalyzed(LocalDateTime.now())
                        .build();
                passwordAnalysisRepository.save(analysis);
            }
        } catch (Exception e) {
            log.error("Failed to analyze password entry {}: {}", payload != null ? payload.getId() : "null", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private int computeStrengthScore(String password) {
        if (password == null || password.isBlank()) return 0;
        int score = 0;
        int len = password.length();
        if (len >= 16) score += 30;
        else if (len >= 12) score += 20;
        else if (len >= 8)  score += 10;
        if (password.chars().anyMatch(Character::isUpperCase)) score += 15;
        if (password.chars().anyMatch(Character::isLowerCase)) score += 15;
        if (password.chars().anyMatch(Character::isDigit))     score += 15;
        if (password.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) score += 25;
        return Math.min(score, 100);
    }

    private List<String> computeIssues(String password) {
        List<String> issues = new ArrayList<>();
        if (password == null || password.length() < 8) { issues.add("Too short"); return issues; }
        if (password.length() < 12) issues.add("Consider using 12+ characters");
        if (password.chars().noneMatch(Character::isUpperCase)) issues.add("Add uppercase letters");
        if (password.chars().noneMatch(Character::isDigit)) issues.add("Add numbers");
        if (password.chars().noneMatch(c -> !Character.isLetterOrDigit(c))) issues.add("Add special characters");
        return issues;
    }

    // ── Inner DTO matching SecurityClient.VaultEntryPayload ─────────────────

    public static class VaultEntryPayload {
        private Long id;
        private String title;
        private String username;
        private String password;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
