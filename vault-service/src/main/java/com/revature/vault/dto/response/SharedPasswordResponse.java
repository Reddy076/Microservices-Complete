package com.revature.vault.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedPasswordResponse {

    private String title;
    private String username;
    /** The password re-encrypted with the one-time key — client decrypts with the fragment key */
    private String encryptedPassword;
    /** IV needed by client to decrypt */
    private String encryptionIv;
    private String websiteUrl;
    private String permission;
    private int viewsRemaining;
    private LocalDateTime expiresAt;
    private String sharedBy;
}
