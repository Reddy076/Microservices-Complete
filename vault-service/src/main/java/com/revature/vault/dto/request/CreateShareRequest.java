package com.revature.vault.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShareRequest {

    @NotNull(message = "Vault entry ID is required")
    private Long vaultEntryId;

    /** Optional — if set, only this email can claim the share */
    private String recipientEmail;

    /** Hours until the share expires. Defaults to 24 if not provided. */
    @Min(value = 1, message = "Expiry must be at least 1 hour")
    @Builder.Default
    private int expiryHours = 24;

    /** Maximum number of times the password can be viewed. Defaults to 1. */
    @Min(value = 1, message = "Max views must be at least 1")
    @Builder.Default
    private int maxViews = 1;

    /** VIEW_ONCE, VIEW_MULTIPLE, or TEMPORARY_ACCESS */
    @Builder.Default
    private String permission = "VIEW_ONCE";
}
