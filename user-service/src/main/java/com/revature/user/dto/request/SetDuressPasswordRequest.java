package com.revature.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetDuressPasswordRequest {

  @NotBlank(message = "Duress password cannot be blank")
  private String duressPassword;
}
