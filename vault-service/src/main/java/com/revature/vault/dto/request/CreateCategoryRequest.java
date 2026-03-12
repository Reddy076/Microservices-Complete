package com.revature.vault.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryRequest {

  @NotBlank(message = "Category name is required")
  @Size(min = 1, max = 100, message = "Category name must be between 1 and 100 characters")
  private String name;

  @Size(max = 50, message = "Icon name must be at most 50 characters")
  private String icon;
}
