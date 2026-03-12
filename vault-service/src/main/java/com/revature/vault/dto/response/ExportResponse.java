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
public class ExportResponse {
  private String fileName;
  private String format;
  private int entryCount;
  private boolean encrypted;
  private String data;
  private LocalDateTime exportedAt;
}


