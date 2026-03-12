package com.revature.vault.service.sharing;

import com.revature.vault.model.sharing.SecureShare;
import com.revature.vault.model.sharing.SharePermission;
import com.revature.vault.repository.SecureShareRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service responsible for all share-expiration business logic.
 *
 * <p>Gap analysis (Feature 35) identified that this class was proposed in the spec but
 * its responsibilities were previously split between the inline {@code SecureShare.isValid()}
 * domain method and {@code SecureShareCleanupScheduler}. Extracting expiration logic here
 * gives a single, testable unit that the scheduler, the retrieval flow, and admin tooling
 * can all delegate to.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Determine whether a {@link SecureShare} is still valid for access.</li>
 *   <li>Calculate the time remaining until a share expires.</li>
 *   <li>Bulk-expire shares that have passed their {@code expiresAt} timestamp.</li>
 *   <li>Apply permission-specific view-limit logic (VIEW_ONCE / VIEW_MULTIPLE / TEMPORARY_ACCESS).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ShareExpirationService {

    private static final Logger logger = LoggerFactory.getLogger(ShareExpirationService.class);

    private final SecureShareRepository shareRepository;

    // ── Validity checks ───────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the share is currently accessible:
     * not revoked, not past its expiry timestamp, and within its allowed view count.
     *
     * @param share the share to check
     * @return {@code true} if the share may still be accessed
     */
    public boolean isValid(SecureShare share) {
        if (share == null) return false;
        if (share.isRevoked()) return false;
        if (LocalDateTime.now().isAfter(share.getExpiresAt())) return false;
        return !isViewLimitReached(share);
    }

    /**
     * Returns {@code true} if the share has reached its maximum permitted view count.
     * {@link SharePermission#TEMPORARY_ACCESS} shares are treated as having unlimited views.
     *
     * @param share the share to check
     * @return {@code true} if no further views are allowed due to view count
     */
    public boolean isViewLimitReached(SecureShare share) {
        if (share.getPermission() == SharePermission.TEMPORARY_ACCESS) {
            return false; // unlimited views while within the time window
        }
        return share.getViewCount() >= share.getMaxViews();
    }

    /**
     * Returns {@code true} if the share's expiry timestamp is in the past.
     *
     * @param share the share to check
     * @return {@code true} if the share has expired
     */
    public boolean isExpired(SecureShare share) {
        return LocalDateTime.now().isAfter(share.getExpiresAt());
    }

    // ── Time-remaining calculation ────────────────────────────────────────────

    /**
     * Returns the number of seconds remaining until this share expires.
     * Returns 0 if the share has already expired.
     *
     * @param share the share to inspect
     * @return seconds until expiry, or 0 if already expired
     */
    public long secondsUntilExpiry(SecureShare share) {
        long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), share.getExpiresAt());
        return Math.max(0, seconds);
    }

    /**
     * Returns the number of views remaining for this share.
     * Returns {@link Integer#MAX_VALUE} for {@link SharePermission#TEMPORARY_ACCESS} shares.
     *
     * @param share the share to inspect
     * @return number of views remaining
     */
    public int viewsRemaining(SecureShare share) {
        if (share.getPermission() == SharePermission.TEMPORARY_ACCESS) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, share.getMaxViews() - share.getViewCount());
    }

    // ── Expiration reason ─────────────────────────────────────────────────────

    /**
     * Returns a human-readable reason why a share is no longer valid,
     * or {@code null} if the share is still valid.
     *
     * @param share the share to check
     * @return invalidation reason string, or {@code null} if valid
     */
    public String getInvalidReason(SecureShare share) {
        if (share.isRevoked()) return "revoked";
        if (isExpired(share)) return "expired";
        if (isViewLimitReached(share)) return "view limit reached";
        return null;
    }

    // ── Bulk expiration (used by scheduler) ───────────────────────────────────

    /**
     * Hard-deletes all shares whose {@code expiresAt} timestamp is before
     * the given cutoff and that have not been manually revoked.
     * Called by {@code SecureShareCleanupScheduler} on a scheduled basis.
     *
     * @param olderThan cutoff timestamp — shares expiring before this are deleted
     * @return the number of shares that were deleted
     */
    @Transactional
    public int deleteExpiredShares(LocalDateTime olderThan) {
        List<SecureShare> expired = shareRepository.findByExpiresAtBeforeAndIsRevokedFalse(olderThan);
        if (expired.isEmpty()) {
            return 0;
        }
        shareRepository.deleteAll(expired);
        logger.info("Deleted {} expired secure shares (older than {})", expired.size(), olderThan);
        return expired.size();
    }

    /**
     * Convenience overload — deletes shares expired before right now minus the given hours.
     *
     * @param graceHours how many hours after expiry to wait before deleting
     * @return the number of shares deleted
     */
    @Transactional
    public int deleteExpiredSharesOlderThan(int graceHours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(graceHours);
        return deleteExpiredShares(cutoff);
    }
}
