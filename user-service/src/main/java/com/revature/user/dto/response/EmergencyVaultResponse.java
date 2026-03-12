package com.revature.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Feature 39 – Emergency Access (Digital Legacy).
 *
 * <p>Response for {@code GET /api/emergency/vault/{token}} — the read-only vault
 * view returned to an approved emergency contact.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyVaultResponse {

    /** Username of the vault owner. */
    private String ownerUsername;

    /** When this access token expires. */
    private LocalDateTime expiresAt;

    /** Hours remaining before the access token expires. */
    private long hoursUntilExpiry;

    /** Read-only vault entries (passwords are NOT decrypted — only metadata). */
    private List<EmergencyVaultEntry> entries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmergencyVaultEntry {
        private Long entryId;
        private String title;
        private String websiteUrl;
        private String categoryName;
        private String folderName;
        private LocalDateTime lastUpdatedAt;
        /** Note: password is intentionally omitted — emergency access is read-only metadata only. */
    }
}
