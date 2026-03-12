package com.revature.security.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAlert {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id")
    private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "alert_type", nullable = false, length = 50)
  private AlertType alertType;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, length = 1000)
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private Severity severity = Severity.MEDIUM;

  @Column(name = "is_read")
  @Builder.Default
  private boolean isRead = false;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public enum AlertType {
    NEW_DEVICE_LOGIN,
    NEW_LOCATION_LOGIN,
    MULTIPLE_FAILED_LOGINS,
    PASSWORD_CHANGED,
    TWO_FA_ENABLED,
    TWO_FA_DISABLED,
    ACCOUNT_LOCKED,
    SENSITIVE_ACCESS,
    PASSWORD_BREACHED,
    BREACH_SCAN_COMPLETE
  }

  public enum Severity {
    LOW, MEDIUM, HIGH, CRITICAL
  }
}


