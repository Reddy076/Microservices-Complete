package com.revature.vault.repository;

import com.revature.vault.model.sharing.SecureShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SecureShareRepository extends JpaRepository<SecureShare, Long> {

       Optional<SecureShare> findByShareToken(String shareToken);

       /** All active (non-revoked, non-expired) shares created by a user */
       @Query("SELECT s FROM SecureShare s WHERE s.ownerId = :ownerId " +
                     "AND s.isRevoked = false AND s.expiresAt > :now ORDER BY s.createdAt DESC")
       List<SecureShare> findActiveSharesByOwnerId(@Param("ownerId") Long ownerId,
                     @Param("now") LocalDateTime now);

       /** All shares (including revoked/expired) for history */
       List<SecureShare> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

       /** Shares directed at a specific recipient email */
       @Query("SELECT s FROM SecureShare s WHERE s.recipientEmail = :email " +
                     "AND s.isRevoked = false AND s.expiresAt > :now ORDER BY s.createdAt DESC")
       List<SecureShare> findActiveSharesForRecipient(@Param("email") String email,
                     @Param("now") LocalDateTime now);

       /** Cleanup — find shares expired before a given time */
       List<SecureShare> findByExpiresAtBeforeAndIsRevokedFalse(LocalDateTime before);

       boolean existsByShareToken(String shareToken);

       void deleteByVaultEntryId(Long vaultEntryId);
}

