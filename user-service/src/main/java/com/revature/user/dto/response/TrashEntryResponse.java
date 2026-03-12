package com.revature.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrashEntryResponse {
  private Long id;
  private String title;
  private String websiteUrl;
  private String categoryName;
  private String folderName;
  private LocalDateTime deletedAt;
  private LocalDateTime expiresAt;
  private long daysRemaining;
}
