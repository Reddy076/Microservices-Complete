package com.revature.vault.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyMasterPasswordRequest {
  @NotBlank(message = "Master password is required")
  private String masterPassword;
}
