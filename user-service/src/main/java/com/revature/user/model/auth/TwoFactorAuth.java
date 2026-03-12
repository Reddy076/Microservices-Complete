package com.revature.user.model.auth;

import com.revature.user.model.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "two_factor_auth")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorAuth {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(name = "secret_key", nullable = false)
  private String secretKey;

  @Column(name = "is_enabled", nullable = false)
  @Builder.Default
  private boolean isEnabled = false;

  @ElementCollection
  @CollectionTable(name = "backup_codes", joinColumns = @JoinColumn(name = "two_factor_auth_id"))
  @Column(name = "code")
  @Builder.Default
  private List<String> recoveryCodes = new ArrayList<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
