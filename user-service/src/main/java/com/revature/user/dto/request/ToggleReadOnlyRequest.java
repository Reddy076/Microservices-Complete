package com.revature.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ToggleReadOnlyRequest {
  @NotNull(message = "readOnlyMode is required")
  private Boolean readOnlyMode;
}
