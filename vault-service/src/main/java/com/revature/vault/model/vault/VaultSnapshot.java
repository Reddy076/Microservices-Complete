package com.revature.vault.model.vault;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vault_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaultSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vault_entry_id", nullable = false)
  private VaultEntry vaultEntry;

  @Column(nullable = false, length = 1000)
  private String password;

  @Column(name = "changed_at", nullable = false)
  private LocalDateTime changedAt;
}
