package com.revature.user.dto.request;

import com.revature.user.dto.SecurityQuestionDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class VerifySecurityQuestionsRequest {

  @NotBlank(message = "Username is required")
  private String username;

  @NotNull(message = "Security answers are required")
  @Size(min = 3, max = 3, message = "You must provide exactly 3 security answers")
  private List<SecurityQuestionDTO> securityAnswers;
}
