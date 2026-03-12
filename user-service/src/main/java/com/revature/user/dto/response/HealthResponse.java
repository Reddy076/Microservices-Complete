package com.revature.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponse {
  private String status;
  private String version;
  private LocalDateTime timestamp;
  private Map<String, ComponentHealth> components;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ComponentHealth {
    private String status;
    private String details;
  }
}
