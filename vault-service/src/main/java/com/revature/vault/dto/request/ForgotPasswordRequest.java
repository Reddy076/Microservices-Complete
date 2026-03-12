package com.revature.vault.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
  @NotBlank(message = "Username or Email is required")
  private String username;
}
