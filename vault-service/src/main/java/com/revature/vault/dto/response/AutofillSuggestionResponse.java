package com.revature.vault.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Feature 36 – Smart Password Autofill (Backend API).
 *
 * <p>Response for {@code POST /api/autofill/suggestions}.
 * Contains matching vault entries for the given domain.
 * <strong>Passwords are NEVER included</strong> — only metadata.
 * The extension must call {@code POST /api/vault/entries/{id}/view-password}
 * separately when the user confirms autofill.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutofillSuggestionResponse {

    /** The extracted domain from the request URL. */
    private String domain;

    /** Matching vault entries, ordered by relevance (exact > subdomain > partial). */
    private List<AutofillEntry> suggestions;

    /** Total number of suggestions found. */
    private int totalCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AutofillEntry {

        /** Vault entry id — used to fetch the password when the user confirms autofill. */
        private Long entryId;

        /** Entry title (e.g., "GitHub"). */
        private String title;

        /** Encrypted username (the extension will display this as a hint). */
        private String username;

        /** The stored website URL. */
        private String websiteUrl;

        /** Match type: EXACT, SUBDOMAIN, or PARTIAL. */
        private String matchType;

        /** Whether this entry is marked as a favorite. */
        private Boolean isFavorite;
    }
}
