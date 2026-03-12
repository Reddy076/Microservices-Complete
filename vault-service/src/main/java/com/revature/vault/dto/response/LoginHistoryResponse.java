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
public class LoginHistoryResponse {
  private Long id;
  private String ipAddress;
  private String deviceInfo;
  private String location;
  private boolean successful;
  private String failureReason;
  private LocalDateTime timestamp;
}
