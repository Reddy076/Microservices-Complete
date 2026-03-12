package com.revature.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import com.revature.user.dto.SecurityQuestionDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request object for registering a new user")
public class RegistrationRequest {

  @Schema(description = "User's email address", example = "jane.doe@example.com")
  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  private String email;

  @Schema(description = "User's chosen display name or login handle", example = "jane_doe22")
  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  private String username;

  @Schema(description = "The highly secure master password for the vault", example = "MySuperSecretPassword456!")
  @NotBlank(message = "Master password is required")
  @Size(min = 12, message = "Master password must be at least 12 characters long")
  private String masterPassword;

  @Schema(description = "Array of 3 security questions/answers for account recovery")
  @jakarta.validation.Valid
  @NotNull(message = "Security questions are required")
  @Size(min = 3, max = 3, message = "You must provide exactly 3 security questions")
  private List<SecurityQuestionDTO> securityQuestions;

  @Schema(description = "Optional hint to remember the master password", example = "My favorite childhood pet")
  @Size(max = 500, message = "Password hint must not exceed 500 characters")
  private String passwordHint;
}
