package com.revature.user.dto.request;

import com.revature.user.dto.SecurityQuestionDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request object for recovering a master password via security questions")
public class RecoveryRequest {

  @Schema(description = "The registered email or username", example = "john.doe@example.com")
  @NotBlank(message = "Username is required")
  private String username;

  @Schema(description = "Array of 3 correctly answered security questions")
  @NotNull(message = "Security answers are required")
  @Size(min = 3, max = 3, message = "You must provide exactly 3 security answers")
  private List<SecurityQuestionDTO> securityAnswers;

  @Schema(description = "The new master password to set", example = "MyNewStrongPassword789!")
  @NotBlank(message = "New password is required")
  @Size(min = 12, message = "Password must be at least 12 characters long")
  private String newMasterPassword;
}
