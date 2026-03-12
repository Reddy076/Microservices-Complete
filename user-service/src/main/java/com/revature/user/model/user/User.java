package com.revature.user.model.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(name = "name")
  private String name;

  @Column(name = "phone_number")
  private String phoneNumber;

  @Column(name = "master_password_hash", nullable = false)
  private String masterPasswordHash;

  @Column(nullable = false)
  private String salt;

  @Column(name = "is_2fa_enabled")
  private boolean is2faEnabled = false;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "deletion_requested_at")
  private LocalDateTime deletionRequestedAt;

  @Column(name = "deletion_scheduled_at")
  private LocalDateTime deletionScheduledAt;

  @Column(name = "failed_login_attempts")
  @Builder.Default
  private int failedLoginAttempts = 0;

  @Column(name = "locked_until")
  private LocalDateTime lockedUntil;

  @Column(name = "lockout_count")
  @Builder.Default
  private int lockoutCount = 0;

  @Column(name = "duress_password_hash")
  private String duressPasswordHash;

  @Column(name = "password_hint", length = 500)
  private String passwordHint;

  @Column(name = "email_verified")
  @Builder.Default
  private Boolean emailVerified = false;
}
