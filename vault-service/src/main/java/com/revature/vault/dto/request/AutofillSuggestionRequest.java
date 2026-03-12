package com.revature.vault.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Feature 36 – Smart Password Autofill (Backend API).
 *
 * <p>Request body for {@code POST /api/autofill/suggestions}.
 * The browser extension sends the current page URL; the backend extracts
 * the domain and returns matching vault entries.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutofillSuggestionRequest {

    /**
     * The full URL of the current page (e.g., "https://github.com/login").
     * The backend will extract the domain for matching.
     */
    @NotBlank(message = "URL is required")
    private String url;
}
