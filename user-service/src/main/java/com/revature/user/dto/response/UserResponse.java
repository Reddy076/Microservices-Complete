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
public class UserResponse {
  private Long id;
  private String email;
  private String username;
  private String name;
  private String phoneNumber;
  private boolean is2faEnabled;
  private LocalDateTime createdAt;
  private LocalDateTime deletionScheduledAt;
}
