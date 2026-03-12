package com.revature.security.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAuditResponse {
  private int totalEntries;
  private int weakCount;
  private int reusedCount;
  private int oldCount;
  private int securityScore;
  private List<String> recommendations;
  private List<VaultEntrySummary> weakPasswords;
  private List<VaultEntrySummary> reusedPasswords;
  private List<VaultEntrySummary> oldPasswords;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VaultEntrySummary {
    private Long id;
    private String title;
    private String websiteUrl;
    private String issue;
  }
}

