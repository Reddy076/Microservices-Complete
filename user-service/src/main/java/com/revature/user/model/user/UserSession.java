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
@Table(name = "user_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, unique = true, length = 2048)
  private String token;

  @Column(name = "ip_address")
  private String ipAddress;

  @Column(name = "device_info")
  private String deviceInfo;

  @Column(name = "device_fingerprint", length = 255)
  private String deviceFingerprint;

  @Column(name = "location")
  private String location;

  @Column(name = "is_active")
  private boolean isActive = true;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "last_accessed_at")
  private LocalDateTime lastAccessedAt;

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;
}
