package com.revature.vault.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for accessing a highly sensitive vault entry")
public class SensitiveAccessRequest {

  @Schema(description = "The user's master password to prove identity", example = "MyStrongPassword123!")
  @NotBlank(message = "Master password is required")
  private String masterPassword;

  @Schema(description = "Optional 2FA OTP token if the user has 2FA enabled", example = "123456")
  private String otpToken;
}
