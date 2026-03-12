package com.revature.user.model.security;

import com.revature.user.model.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "username_attempted", nullable = false)
  private String usernameAttempted;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Column(name = "device_info", length = 500)
  private String deviceInfo;

  @Column(length = 200)
  private String location;

  @Column(nullable = false)
  private boolean successful;

  @Column(name = "failure_reason", length = 200)
  private String failureReason;

  @Column(name = "risk_score")
  private Integer riskScore;

  @Column(nullable = false)
  private LocalDateTime timestamp;
}
