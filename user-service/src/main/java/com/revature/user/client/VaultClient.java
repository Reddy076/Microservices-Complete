package com.revature.user.client;

import org.springframework.cloud.openfeign.FeignClient;
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
}
