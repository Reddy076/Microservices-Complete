package com.revature.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsResponse {
  private String theme;
  private String language;
  private Integer autoLogoutMinutes;
  private Boolean readOnlyMode;
}
