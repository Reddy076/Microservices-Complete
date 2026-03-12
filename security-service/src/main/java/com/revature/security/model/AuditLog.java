package com.revature.security.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id")
    private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
  private AuditAction action;

  @Column(length = 500)
  private String details;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Column(nullable = false)
  private LocalDateTime timestamp;

  public enum AuditAction {
    LOGIN,
    LOGIN_FAILED,
    LOGOUT,
    ENTRY_CREATED,
    ENTRY_UPDATED,
    ENTRY_DELETED,
    PASSWORD_VIEWED,
    ENTRY_RESTORED,
    VAULT_EXPORTED,
    // Feature 33 — Dashboard
    DASHBOARD_VIEWED,
    // Feature 34 — Breach Monitor
    BREACH_SCAN_RUN,
    BREACH_DETECTED,
    BREACH_RESOLVED,
    // Feature 35 — Secure Sharing
    SHARE_CREATED,
    SHARE_ACCESSED,
    SHARE_REVOKED,
    // Feature 37 — Vault Timeline
    TIMELINE_VIEWED
  }
}


