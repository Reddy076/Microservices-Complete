package com.revature.security.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import com.revature.security.util.StringListConverter;

@Entity
@Table(name = "password_analysis")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordAnalysis {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "vault_entry_id")
    private Long vaultEntryId;

  @Column(name = "strength_score")
  private int strengthScore;

  @Column(name = "is_reused")
  private boolean isReused;

  @Column(name = "issues", length = 1000)
  @Convert(converter = StringListConverter.class)
  private List<String> issues;

  @Column(name = "last_analyzed", nullable = false)
  private LocalDateTime lastAnalyzed;
}





