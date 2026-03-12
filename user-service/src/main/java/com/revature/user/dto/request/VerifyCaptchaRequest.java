package com.revature.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyCaptchaRequest {
  @NotBlank(message = "Captcha token is required")
  private String captchaToken;
}
