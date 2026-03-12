package com.revature.vault.model.vault;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "vault_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaultEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  @lombok.ToString.Exclude
  private Category category;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "folder_id")
  @lombok.ToString.Exclude
  private Folder folder;

  @Column(name = "account_name", nullable = false)
  private String title;

  @Column(nullable = false)
  private String username;

  @Column(name = "encrypted_password", nullable = false, length = 1000)
  private String password;

  @Column(name = "website_url")
  private String websiteUrl;

  @Column(length = 2000)
  private String notes;

  @Column(name = "is_favorite")
  @Builder.Default
  private Boolean isFavorite = false;

  @Column(name = "is_highly_sensitive")
  @Builder.Default
  private Boolean isHighlySensitive = false;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "is_deleted")
  @Builder.Default
  private Boolean isDeleted = false;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
