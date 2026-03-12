package com.revature.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportRequest {
  private String data;
  private String format;
  private String password; // optional — only needed for encrypted exports
}
