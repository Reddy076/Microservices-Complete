package com.revature.vault.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetPasswordHintRequest {

  @NotBlank(message = "Master password is required to set a hint")
  private String masterPassword;

  private String hint;
}
