package com.revature.vault.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaultEntryDetailResponse {
  private Long id;
  private String title;
  private String username;
  private String password;
  private String websiteUrl;
  private String notes;
  private Long categoryId;
  private String categoryName;
  private Long folderId;
  private String folderName;
  private Boolean isFavorite;
  private Boolean isHighlySensitive;
  private Boolean requiresSensitiveAuth;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Integer strengthScore;
  private String strengthLabel;
}
