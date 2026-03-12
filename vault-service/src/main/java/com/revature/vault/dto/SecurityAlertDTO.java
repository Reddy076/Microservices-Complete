package com.revature.vault.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAlertDTO {
  private Long id;
  private String alertType;
  private String title;
  private String message;
  private String severity;
  private boolean isRead;
  private LocalDateTime createdAt;
}
