package com.revature.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "vault-service")
public interface VaultClient {

    @PostMapping("/internal/vault/users/{userId}/default-folders-categories")
    void createDefaultFoldersAndCategories(@PathVariable("userId") Long userId);

    @PostMapping("/internal/vault/users/{userId}/reencrypt")
    void reencryptVault(@PathVariable("userId") Long userId, @RequestBody Map<String, String> keys);

    /** Called by UserController.getDashboard() to get tile counts. */
    @GetMapping("/internal/vault/stats/{userId}")
    VaultStatsResponse getDashboardStats(@PathVariable("userId") Long userId);

    class VaultStatsResponse {
        private int vaultCount;
        private int favoriteCount;
        private int trashCount;

        public int getVaultCount() { return vaultCount; }
        public void setVaultCount(int vaultCount) { this.vaultCount = vaultCount; }
        public int getFavoriteCount() { return favoriteCount; }
        public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }
        public int getTrashCount() { return trashCount; }
        public void setTrashCount(int trashCount) { this.trashCount = trashCount; }
    }
}
