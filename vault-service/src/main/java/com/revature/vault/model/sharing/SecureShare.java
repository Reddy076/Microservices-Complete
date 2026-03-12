package com.revature.vault.model.sharing;

import com.revature.vault.model.vault.VaultEntry;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a secure, time-limited password share.
 *
 * <p>Security design:
 * <ul>
 *   <li>The {@code encryptedPassword} is re-encrypted with a random one-time AES-256 key.</li>
 *   <li>That key is embedded in the share URL fragment (e.g. {@code /share/{token}#key}) so
 *       it is <em>never</em> sent to the server on retrieval.</li>
 *   <li>The server stores only the ciphertext; even if the DB is compromised the attacker
 *       cannot decrypt without the fragment key.</li>
 * </ul>
 */
@Entity
@Table(name = "secure_shares")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecureShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vault_entry_id", nullable = false)
    @lombok.ToString.Exclude
    private VaultEntry vaultEntry;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** Optional — null means "anyone with the link can access" */
    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    /** UUID-based token used in the share URL path */
    @Column(name = "share_token", unique = true, nullable = false, length = 255)
    private String shareToken;

    /** Password re-encrypted with a one-time AES-256 key (key lives only in URL fragment) */
    @Column(name = "encrypted_password", nullable = false, columnDefinition = "TEXT")
    private String encryptedPassword;

    /** IV used for the one-time encryption, stored alongside ciphertext */
    @Column(name = "encryption_iv", length = 255)
    private String encryptionIv;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private int viewCount = 0;

    @Column(name = "max_views", nullable = false)
    @Builder.Default
    private int maxViews = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SharePermission permission = SharePermission.VIEW_ONCE;

    @Column(name = "is_revoked", nullable = false)
    @Builder.Default
    private boolean isRevoked = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Returns true if this share is still usable */
    public boolean isValid() {
        return !isRevoked
                && LocalDateTime.now().isBefore(expiresAt)
                && viewCount < maxViews;
    }
}
