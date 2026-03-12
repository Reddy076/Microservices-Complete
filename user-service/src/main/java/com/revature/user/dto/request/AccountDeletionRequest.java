package com.revature.user.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AccountDeletionRequest {

  @NotBlank(message = "Master password is required")
  private String masterPassword;

  @AssertTrue(message = "You must confirm deletion")
  private boolean confirmation;
}
