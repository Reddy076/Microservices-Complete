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
public class SessionResponse {
  private Long id;
  private String ipAddress;
  private String deviceInfo;
  private String location;
  private boolean isActive;
  private LocalDateTime createdAt;
  private LocalDateTime lastAccessedAt;
  private LocalDateTime expiresAt;
}
