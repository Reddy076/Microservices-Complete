package com.revature.generator.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordGeneratorRequest {
  @Builder.Default
  private int length = 16;

  @Builder.Default
  private boolean includeUppercase = true;

  @Builder.Default
  private boolean includeLowercase = true;

  @Builder.Default
  private boolean includeNumbers = true;

  @Builder.Default
  private boolean includeSpecial = true;

  @Builder.Default
  private boolean excludeSimilar = false;

  @Builder.Default
  private boolean excludeAmbiguous = false;

  @Builder.Default
  private int count = 1;
}

