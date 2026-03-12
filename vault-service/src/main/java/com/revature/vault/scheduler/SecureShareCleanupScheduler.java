package com.revature.vault.scheduler;

import com.revature.vault.model.sharing.SecureShare;
import com.revature.vault.repository.SecureShareRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled cleanup of expired secure shares.
 * Adapted from the monolith's SecureShareCleanupScheduler — package changes only.
 * Runs every hour to delete shares that expired more than 24 hours ago.
 */
@Component
@RequiredArgsConstructor
public class SecureShareCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SecureShareCleanupScheduler.class);

    private final SecureShareRepository shareRepository;

    @Scheduled(cron = "0 0 * * * ?") // Every hour at :00
    @Transactional
    public void cleanupExpiredShares() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<SecureShare> expired = shareRepository.findByExpiresAtBeforeAndIsRevokedFalse(cutoff);

        if (!expired.isEmpty()) {
            shareRepository.deleteAll(expired);
            logger.info("Cleaned up {} expired secure share(s) older than {}.", expired.size(), cutoff);
        }
    }
}
