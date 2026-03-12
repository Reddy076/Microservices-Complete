package com.revature.security.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import lombok.Data;
import java.time.LocalDateTime;

@FeignClient(name = "vault-service")
public interface VaultAnalysisClient {
    @GetMapping("/internal/vault/{userId}/decrypted-entries")
    List<DecryptedVaultEntryDTO> getDecryptedEntries(@PathVariable("userId") Long userId);

    @Data
    class DecryptedVaultEntryDTO {
        private Long entryId;
        private String title;
        private String websiteUrl;
        private Long categoryId;
        private String categoryName;
        private Boolean isFavorite;
        private String decryptedPassword;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
