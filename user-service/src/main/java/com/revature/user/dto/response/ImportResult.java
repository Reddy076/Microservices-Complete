package com.revature.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {
  private int totalProcessed;
  private int successCount;
  private int failCount;
  private String message;
}
