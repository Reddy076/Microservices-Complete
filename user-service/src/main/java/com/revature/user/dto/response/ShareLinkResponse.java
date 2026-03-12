package com.revature.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareLinkResponse {

    private Long shareId;
    private String shareToken;
    /** Full URL safe to share: /api/shares/{token}  */
    private String shareUrl;
    /** The one-time AES key — must be appended as URL fragment by the client: #<encryptionKey> */
    private String encryptionKey;
    private String vaultEntryTitle;
    private String recipientEmail;
    private String permission;
    private int maxViews;
    private int viewCount;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private boolean revoked;
}
