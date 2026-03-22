package com.revature.user.controller;

import com.revature.user.exception.ResourceNotFoundException;
import com.revature.user.model.user.User;
import com.revature.user.repository.TwoFactorAuthRepository;
import com.revature.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Internal endpoints called by other microservices via Feign clients.
 * These are permitAll in SecurityConfig - auth is handled at the API Gateway.
 *
 * Supported Feign URL patterns:
 *  - vault-service:    GET /api/users/internal/by-username/{username}
 *  - vault-service:    GET /api/users/internal/by-id/{id}
 *  - security-service: GET /internal/users/{username}
 *  - security-service: GET /internal/users/email/{email}
 */
@RestController
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;
    private final TwoFactorAuthRepository twoFactorAuthRepository;

    // ── vault-service Feign endpoints ──────────────────────────────────────

    @GetMapping("/api/users/internal/by-username/{username}")
    public ResponseEntity<Map<String, Object>> getUserByUsername(@PathVariable String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return ResponseEntity.ok(toVaultMap(user));
    }

    @GetMapping("/api/users/internal/by-id/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return ResponseEntity.ok(toVaultMap(user));
    }

    // ── security-service Feign endpoints ───────────────────────────────────

    @GetMapping("/internal/users/{username}")
    public ResponseEntity<Map<String, Object>> getUserByUsernameInternal(@PathVariable String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return ResponseEntity.ok(toSecurityMap(user));
    }

    @GetMapping("/internal/users/email/{email}")
    public ResponseEntity<Map<String, Object>> getUserByEmailInternal(@PathVariable String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return ResponseEntity.ok(toSecurityMap(user));
    }

    // ── Response mappers ───────────────────────────────────────────────────

    /** Full details for vault-service (needs masterPasswordHash, salt, etc.) */
    private Map<String, Object> toVaultMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("masterPasswordHash", user.getMasterPasswordHash() != null ? user.getMasterPasswordHash() : "");
        map.put("salt", user.getSalt() != null ? user.getSalt() : "");
        map.put("is2faEnabled", user.is2faEnabled());
        map.put("readOnlyMode", false);

        // Include TOTP secret so vault-service can verify OTP for sensitive entry unlock
        if (user.is2faEnabled()) {
            twoFactorAuthRepository.findByUser(user).ifPresent(tfa ->
                map.put("twoFactorSecret", tfa.getSecretKey())
            );
        }
        return map;
    }

    /** Minimal details for security-service (id, username, email, createdAt) */
    private Map<String, Object> toSecurityMap(User user) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail() != null ? user.getEmail() : "");
        map.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
        return map;
    }
}
