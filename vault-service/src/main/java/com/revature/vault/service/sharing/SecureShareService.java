package com.revature.vault.service.sharing;

import com.revature.vault.dto.request.CreateShareRequest;
import com.revature.vault.dto.response.ShareLinkResponse;
import com.revature.vault.dto.response.SharedPasswordResponse;
import com.revature.vault.exception.ResourceNotFoundException;
import com.revature.vault.model.sharing.SecureShare;
import com.revature.vault.model.sharing.SharePermission;
import com.revature.vault.client.UserClient.UserVaultDetails;
import com.revature.vault.model.vault.VaultEntry;


import com.revature.vault.repository.SecureShareRepository;
import com.revature.vault.client.UserClient;
import com.revature.vault.repository.VaultEntryRepository;

import com.revature.vault.client.NotificationClient;
import com.revature.vault.client.SecurityClient;
import com.revature.vault.service.vault.EncryptionService;
import com.revature.vault.service.sharing.ShareEncryptionService.ShareEncryptionResult;
import com.revature.vault.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecureShareService {

    private static final Logger logger = LoggerFactory.getLogger(SecureShareService.class);

    private final SecureShareRepository shareRepository;
    private final UserClient userClient;
    private final VaultEntryRepository vaultEntryRepository;
    private final EncryptionService encryptionService;
    private final EncryptionUtil encryptionUtil;
    private final ShareTokenGenerator tokenGenerator;
    private final ShareEncryptionService shareEncryptionService;
    
    private final NotificationClient notificationClient;
    private final SecurityClient securityClient;

    // ── create ────────────────────────────────────────────────────────────────

    /**
     * Creates a new secure share for a vault entry.
     * Re-encrypts the vault password with a one-time AES key and returns the key
     * to the caller (it must be embedded in the share URL fragment).
     */
    @Transactional
    public ShareLinkResponse createShare(String username, CreateShareRequest request) {
        UserVaultDetails owner = userClient.getUserDetailsByUsername(username);
        VaultEntry entry = vaultEntryRepository.findByIdAndUserId(
                request.getVaultEntryId(), owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vault entry not found"));

        // Gap 14 fix: block sharing of highly sensitive entries
        if (Boolean.TRUE.equals(entry.getIsHighlySensitive())) {
            throw new IllegalArgumentException(
                    "Entry '" + entry.getTitle() + "' is marked as highly sensitive and cannot be shared.");
        }

        // Decrypt the vault password
        SecretKey vaultKey = encryptionUtil.deriveKey(
                owner.getMasterPasswordHash(), owner.getSalt());
        String plainPassword = encryptionService.decrypt(entry.getPassword(), vaultKey);

        // Re-encrypt with a fresh one-time key
        ShareEncryptionResult encrypted = shareEncryptionService.encrypt(plainPassword);

        SharePermission permission;
        try {
            permission = SharePermission.valueOf(
                    request.getPermission() != null ? request.getPermission() : "VIEW_ONCE");
        } catch (IllegalArgumentException e) {
            permission = SharePermission.VIEW_ONCE;
        }

        int maxViews = permission == SharePermission.TEMPORARY_ACCESS
                ? Integer.MAX_VALUE
                : Math.max(1, request.getMaxViews());

        SecureShare share = SecureShare.builder()
                .vaultEntry(entry)
                .ownerId(owner.getId())
                .recipientEmail(request.getRecipientEmail())
                .shareToken(tokenGenerator.generate())
                .encryptedPassword(encrypted.getEncryptedData())
                .encryptionIv(encrypted.getIv())
                .expiresAt(LocalDateTime.now().plusHours(
                        request.getExpiryHours() > 0 ? request.getExpiryHours() : 24))
                .maxViews(maxViews)
                .permission(permission)
                .build();

        share = shareRepository.save(share);
        logger.info("Secure share created: id={} token={} entry={} owner={}",
                share.getId(), share.getShareToken(), entry.getId(), username);

        // Gap 9 fix: email recipient if one was specified
        if (request.getRecipientEmail() != null && !request.getRecipientEmail().isBlank()) {
            String shareUrl = "/api/shares/" + share.getShareToken()
                    + "#" + encrypted.getKeyBase64();
            String expiresAt = share.getExpiresAt().toString();
            // email notification
            // Gap 10 fix: in-app notification for recipient if they are a registered user.
            // Use a final copy of username for lambda capture.
            final String senderUsername = username;
            final SecureShare savedShare = share;
            try {
                UserVaultDetails recipient = userClient.getUserDetailsByUsername(request.getRecipientEmail());
                notificationClient.createNotification(
                        recipient.getUsername(),
                        "ACCOUNT_ACTIVITY",
                        "Secure Password Shared With You",
                        senderUsername + " has shared a secure password link with you. " +
                        "Check your email to access it. It expires at " + savedShare.getExpiresAt() + ".");
            } catch (Exception e) {
                logger.warn("Could not notify recipient: " + e.getMessage());
            }
        }

        // Gap 8 (share audit): log share creation
        securityClient.logAction(username, "SHARE_CREATED",
                "Shared entry '" + entry.getTitle() + "' token=" + share.getShareToken());

        return toShareLinkResponse(share, encrypted.getKeyBase64());
    }

    // ── retrieve (public) ─────────────────────────────────────────────────────

    /**
     * Retrieves a shared password by its token. Increments view count.
     * Does NOT decrypt — returns the ciphertext + IV for client-side decryption.
     */
    @Transactional
    public SharedPasswordResponse getSharedPassword(String token) {
        SecureShare share = shareRepository.findByShareToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Share not found or has expired"));

        if (!share.isValid()) {
            String reason = share.isRevoked() ? "revoked"
                    : share.getViewCount() >= share.getMaxViews() ? "view limit reached"
                    : "expired";
            throw new IllegalStateException("Share is no longer valid: " + reason);
        }

        share.setViewCount(share.getViewCount() + 1);
        shareRepository.save(share);

        VaultEntry entry = share.getVaultEntry();
        int remaining = share.getPermission() == SharePermission.TEMPORARY_ACCESS
                ? Integer.MAX_VALUE
                : share.getMaxViews() - share.getViewCount();

        // Gap 13 fix (CRITICAL): VaultEntry.username is stored encrypted — decrypt it
        // before returning. If decryption fails, fall back to null gracefully.
        String plainUsername = null;
        try {
            Long ownerId = share.getOwnerId();
            UserVaultDetails owner = userClient.getUserDetailsById(ownerId);
            SecretKey vaultKey = encryptionUtil.deriveKey(
                    owner.getMasterPasswordHash(), owner.getSalt());
            plainUsername = encryptionService.decrypt(entry.getUsername(), vaultKey);
        } catch (Exception e) {
            logger.warn("Could not decrypt username for shared entry {}: {}", entry.getId(), e.getMessage());
        }

        // Audit log the share access
        try {
             UserVaultDetails owner = userClient.getUserDetailsById(share.getOwnerId());
             securityClient.logAction(owner.getUsername(), "SHARE_ACCESSED",
                 "Share accessed: token=" + share.getShareToken() + " viewCount=" + share.getViewCount());
        } catch (Exception e) {}

        return SharedPasswordResponse.builder()
                .title(entry.getTitle())
                .username(plainUsername)
                .encryptedPassword(share.getEncryptedPassword())
                .encryptionIv(share.getEncryptionIv())
                .websiteUrl(entry.getWebsiteUrl())
                .permission(share.getPermission().name())
                .viewsRemaining(remaining)
                .expiresAt(share.getExpiresAt())
                .build();
    }

    // ── list active shares (owner) ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ShareLinkResponse> getActiveShares(String username) {
        UserVaultDetails owner = userClient.getUserDetailsByUsername(username);
        return shareRepository.findActiveSharesByOwnerId(owner.getId(), LocalDateTime.now())
                .stream()
                .map(s -> toShareLinkResponse(s, null)) // key not returned on list
                .collect(Collectors.toList());
    }

    // ── revoke ────────────────────────────────────────────────────────────────

    @Transactional
    public ShareLinkResponse revokeShare(String username, Long shareId) {
        UserVaultDetails owner = userClient.getUserDetailsByUsername(username);
        SecureShare share = shareRepository.findById(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Share not found"));

        if (!userClient.getUserDetailsById(share.getOwnerId()).getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Share does not belong to this user");
        }

        share.setRevoked(true);
        shareRepository.save(share);
        securityClient.logAction(username, "SHARE_REVOKED",
                "Share revoked: id=" + shareId + " entry='" + share.getVaultEntry().getTitle() + "'");
        logger.info("Share revoked: id={} by user={}", shareId, username);
        return toShareLinkResponse(share, null);
    }

    // ── received shares ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ShareLinkResponse> getReceivedShares(String username) {
        UserVaultDetails user = userClient.getUserDetailsByUsername(username);
        // Assuming user.getUsername() is the email or we retrieve email from user.
        return shareRepository.findActiveSharesForRecipient(username, LocalDateTime.now())
                .stream()
                .map(s -> toShareLinkResponse(s, null))
                .collect(Collectors.toList());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ShareLinkResponse toShareLinkResponse(SecureShare share, String encryptionKey) {
        return ShareLinkResponse.builder()
                .shareId(share.getId())
                .shareToken(share.getShareToken())
                .shareUrl("/api/shares/" + share.getShareToken())
                .encryptionKey(encryptionKey)
                .vaultEntryTitle(share.getVaultEntry().getTitle())
                .recipientEmail(share.getRecipientEmail())
                .permission(share.getPermission().name())
                .maxViews(share.getMaxViews())
                .viewCount(share.getViewCount())
                .expiresAt(share.getExpiresAt())
                .createdAt(share.getCreatedAt())
                .revoked(share.isRevoked())
                .build();
    }
}



