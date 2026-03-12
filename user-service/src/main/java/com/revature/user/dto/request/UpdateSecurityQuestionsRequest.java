package com.revature.user.dto.request;

import com.revature.user.dto.SecurityQuestionDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateSecurityQuestionsRequest {

  @NotBlank(message = "Master password is required")
  private String masterPassword;

  @NotNull(message = "Security questions are required")
  @Size(min = 3, max = 3, message = "You must provide exactly 3 security questions")
  private List<SecurityQuestionDTO> securityQuestions;
}
