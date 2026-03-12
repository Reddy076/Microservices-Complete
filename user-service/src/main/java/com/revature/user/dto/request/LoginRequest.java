package com.revature.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request object for authenticating a user")
public class LoginRequest {

  @Schema(description = "The registered email or username", example = "john.doe@example.com")
  @NotBlank(message = "Username is required")
  private String username;

  @Schema(description = "The user's master password", example = "MyStrongPassword123!")
  @NotBlank(message = "Master password is required")
  private String masterPassword;

  private String captchaToken;
}
