package com.revature.vault.controller;

import com.revature.vault.client.UserClient;
import com.revature.vault.model.vault.VaultEntry;
import com.revature.vault.repository.VaultEntryRepository;
import com.revature.vault.repository.VaultTrashRepository;
import com.revature.vault.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Internal endpoints for service-to-service communication.
 * - security-service calls /internal/vault/{userId}/decrypted-entries for password analysis
 * - user-service calls /internal/vault/stats/{userId} for dashboard tile counts
 */
@RestController
@RequestMapping("/internal/vault")
@RequiredArgsConstructor
public class VaultInternalController {

    private static final Logger log = LoggerFactory.getLogger(VaultInternalController.class);

    private final VaultEntryRepository vaultEntryRepository;
    private final VaultTrashRepository vaultTrashRepository;
    private final UserClient userClient;
    private final EncryptionUtil encryptionUtil;

    /**
     * Returns vault entries with DECRYPTED passwords for security analysis.
     * Called by security-service's VaultAnalysisClient.
     */
    @GetMapping("/{userId}/decrypted-entries")
    public ResponseEntity<List<DecryptedVaultEntryDTO>> getDecryptedEntries(@PathVariable Long userId) {
        List<VaultEntry> entries = vaultEntryRepository.findByUserIdAndIsDeletedFalse(userId);

        // Derive the per-user decryption key from masterPasswordHash + salt
        SecretKey key = null;
        try {
            UserClient.UserVaultDetails user = userClient.getUserDetailsById(userId);
            if (user != null && user.getMasterPasswordHash() != null && user.getSalt() != null) {
                key = encryptionUtil.deriveKey(user.getMasterPasswordHash(), user.getSalt());
            }
        } catch (Exception e) {
            log.warn("Could not fetch user details for decryption (userId={}): {}", userId, e.getMessage());
        }

        final SecretKey decryptionKey = key;

        List<DecryptedVaultEntryDTO> result = entries.stream()
            .map(e -> {
                DecryptedVaultEntryDTO dto = new DecryptedVaultEntryDTO();
                dto.setEntryId(e.getId());
                dto.setTitle(e.getTitle());
                dto.setWebsiteUrl(e.getWebsiteUrl());
                dto.setCategoryId(e.getCategory() != null ? e.getCategory().getId() : null);
                dto.setCategoryName(e.getCategory() != null ? e.getCategory().getName() : null);
                dto.setIsFavorite(e.getIsFavorite());
                dto.setCreatedAt(e.getCreatedAt());
                dto.setUpdatedAt(e.getUpdatedAt());

                // Decrypt password so security-service can score the actual plaintext
                String decrypted = "";
                if (decryptionKey != null && e.getPassword() != null && !e.getPassword().isBlank()) {
                    try {
                        decrypted = encryptionUtil.decrypt(e.getPassword(), decryptionKey);
                    } catch (Exception ex) {
                        log.warn("Failed to decrypt password for entry {}: {}", e.getId(), ex.getMessage());
                    }
                }
                dto.setDecryptedPassword(decrypted);
                return dto;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Dashboard stats for user-service dashboard endpoint.
     * GET /internal/vault/stats/{userId}
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<VaultStatsResponse> getDashboardStats(@PathVariable Long userId) {
        int vaultCount = vaultEntryRepository.countByUserIdAndIsDeletedFalse(userId);
        int favoriteCount = vaultEntryRepository.countByUserIdAndIsFavoriteTrue(userId);
        long trashCount = vaultTrashRepository.countByUserIdAndIsDeletedTrue(userId);
        return ResponseEntity.ok(new VaultStatsResponse(vaultCount, favoriteCount, (int) trashCount));
    }

    // ─── DTOs ────────────────────────────────────────────────────────────────

    public static class VaultStatsResponse {
        private int vaultCount;
        private int favoriteCount;
        private int trashCount;

        public VaultStatsResponse() {}
        public VaultStatsResponse(int vaultCount, int favoriteCount, int trashCount) {
            this.vaultCount = vaultCount;
            this.favoriteCount = favoriteCount;
            this.trashCount = trashCount;
        }
        public int getVaultCount() { return vaultCount; }
        public void setVaultCount(int vaultCount) { this.vaultCount = vaultCount; }
        public int getFavoriteCount() { return favoriteCount; }
        public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }
        public int getTrashCount() { return trashCount; }
        public void setTrashCount(int trashCount) { this.trashCount = trashCount; }
    }

    public static class DecryptedVaultEntryDTO {
        private Long entryId;
        private String title;
        private String websiteUrl;
        private Long categoryId;
        private String categoryName;
        private Boolean isFavorite;
        private String decryptedPassword;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Long getEntryId() { return entryId; }
        public void setEntryId(Long entryId) { this.entryId = entryId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getWebsiteUrl() { return websiteUrl; }
        public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public Boolean getIsFavorite() { return isFavorite; }
        public void setIsFavorite(Boolean isFavorite) { this.isFavorite = isFavorite; }
        public String getDecryptedPassword() { return decryptedPassword; }
        public void setDecryptedPassword(String decryptedPassword) { this.decryptedPassword = decryptedPassword; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}
