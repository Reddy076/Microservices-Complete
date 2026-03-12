package com.revature.vault.scheduler;

import com.revature.vault.service.vault.VaultTrashService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled cleanup of soft-deleted vault entries past their grace period.
 * Adapted from the monolith's TrashCleanupScheduler — package changes only.
 * Runs daily at midnight.
 */
@Component
@RequiredArgsConstructor
public class TrashCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TrashCleanupScheduler.class);
    private final VaultTrashService vaultTrashService;

    @Scheduled(cron = "0 0 0 * * ?") // Daily at midnight
    public void cleanupExpiredTrash() {
        logger.info("Running scheduled trash cleanup task...");
        vaultTrashService.cleanupExpired();
        logger.info("Trash cleanup task completed.");
    }
}
