package com.revature.vault.service.sharing;

import com.revature.vault.repository.SecureShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Generates collision-free, URL-safe share tokens.
 * Uses UUID v4; retries on the astronomically rare collision.
 */
@Component
@RequiredArgsConstructor
public class ShareTokenGenerator {

    private final SecureShareRepository shareRepository;

    /**
     * Generates a unique share token (UUID v4, hyphens removed for URL cleanliness).
     * Retries up to 5 times in the event of a token collision (should never happen in practice).
     */
    public String generate() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String token = UUID.randomUUID().toString().replace("-", "");
            if (!shareRepository.existsByShareToken(token)) {
                return token;
            }
        }
        throw new IllegalStateException("Failed to generate a unique share token after 5 attempts");
    }
}
