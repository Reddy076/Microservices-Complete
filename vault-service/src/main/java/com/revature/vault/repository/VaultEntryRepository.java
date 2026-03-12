package com.revature.vault.repository;

import com.revature.vault.model.vault.VaultEntry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VaultEntryRepository extends JpaRepository<VaultEntry, Long> {
        List<VaultEntry> findByUserIdAndIsDeletedFalse(Long userId);

        List<VaultEntry> findByUserIdAndFolderId(Long userId, Long folderId);

        List<VaultEntry> findByUserIdAndCategoryId(Long userId, Long categoryId);

        int countByUserId(Long userId);

        int countByUserIdAndIsDeletedFalse(Long userId);

        int countByUserIdAndIsFavoriteTrue(Long userId);

        Optional<VaultEntry> findByIdAndUserId(Long id, Long userId);

        List<VaultEntry> findByUserIdAndFolderIdAndIsDeletedFalse(Long userId, Long folderId);

        List<VaultEntry> findByUserIdAndIsFavoriteTrueAndIsDeletedFalse(Long userId);

        @Query("SELECT v FROM VaultEntry v WHERE v.userId = :userId AND v.isDeleted = false " +
                        "AND (:keyword IS NULL OR LOWER(v.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "    OR LOWER(v.websiteUrl) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "AND (:categoryId IS NULL OR v.category.id = :categoryId) " +
                        "AND (:folderId IS NULL OR v.folder.id = :folderId) " +
                        "AND (:isFavorite IS NULL OR v.isFavorite = :isFavorite) " +
                        "AND (:isHighlySensitive IS NULL OR v.isHighlySensitive = :isHighlySensitive)")
        List<VaultEntry> searchEntries(
                        @Param("userId") Long userId,
                        @Param("keyword") String keyword,
                        @Param("categoryId") Long categoryId,
                        @Param("folderId") Long folderId,
                        @Param("isFavorite") Boolean isFavorite,
                        @Param("isHighlySensitive") Boolean isHighlySensitive);

        List<VaultEntry> findTop10ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);

        List<VaultEntry> findTop10ByUserIdAndIsDeletedFalseOrderByUpdatedAtDesc(Long userId);

        /** Returns ALL entries for a user including deleted ones (for timeline/history lookups). */
        List<VaultEntry> findByUserId(Long userId);
}





