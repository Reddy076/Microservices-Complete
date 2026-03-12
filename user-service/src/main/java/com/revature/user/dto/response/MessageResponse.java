package com.revature.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
  private String message;

  @Builder.Default
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private LocalDateTime timestamp = LocalDateTime.now();

  public MessageResponse(String message) {
    this.message = message;
    this.timestamp = LocalDateTime.now();
  }
}
