package com.revature.user.model.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "recovery_codes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecoveryCode {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "code_hash", nullable = false)
  private String codeHash;

  @Column(name = "is_used", nullable = false)
  private boolean isUsed;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
}
