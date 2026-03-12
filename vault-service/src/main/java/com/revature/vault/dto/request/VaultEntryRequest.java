package com.revature.vault.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for creating or updating a vault entry")
public class VaultEntryRequest {

  @Schema(description = "The title or name of the entry", example = "Netflix Account")
  private String title;

  @Schema(description = "The username or email for the account", example = "jane.doe@example.com")
  private String username;
  @Schema(description = "The password for the account", example = "Sup3rS3cr3tP@ss")
  private String password;

  @Schema(description = "The URL of the website", example = "https://www.netflix.com")
  private String websiteUrl;

  @Schema(description = "Optional notes regarding the account", example = "Family plan shared with John")
  private String notes;

  @Schema(description = "Optional ID of a Category to assign this entry to", example = "1")
  private Long categoryId;

  @Schema(description = "Optional ID of a Folder to place this entry inside", example = "5")
  private Long folderId;

  @Schema(description = "Boolean flag to mark entry as a favorite")
  private Boolean isFavorite;

  @Schema(description = "Boolean flag indicating if the entry is highly sensitive (requires re-prompt of master password)")
  private Boolean isHighlySensitive;
}
