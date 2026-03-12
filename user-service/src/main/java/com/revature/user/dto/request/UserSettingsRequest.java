package com.revature.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsRequest {
  private String theme;
  private String language;
  private Integer autoLogoutMinutes;
  private Boolean readOnlyMode;
}
