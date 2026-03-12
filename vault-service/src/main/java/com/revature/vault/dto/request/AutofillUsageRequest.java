package com.revature.vault.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Feature 36 – Smart Password Autofill (Backend API).
 *
 * <p>Request body for {@code POST /api/autofill/log-usage}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutofillUsageRequest {

    @NotBlank(message = "URL is required")
    private String url;

    /** The vault entry id that was selected (null if no entry was selected). */
    private Long vaultEntryId;

    /** Whether the autofill was actually applied (true) or just suggested (false). */
    private boolean applied;
}
