package com.revature.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
  @Size(max = 100, message = "Name must be less than 100 characters")
  private String name;

  @jakarta.validation.constraints.Email(message = "Providing a valid email address is required")
  @Size(max = 100, message = "Email must be less than 100 characters")
  private String email;

  @Size(max = 20, message = "Phone number must be less than 20 characters")
  private String phoneNumber;
}
