package com.revature.vault.repository;

import com.revature.vault.model.vault.VaultEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VaultTrashRepository extends JpaRepository<VaultEntry, Long> {

  List<VaultEntry> findByUserIdAndIsDeletedTrue(Long userId);

  Optional<VaultEntry> findByIdAndUserIdAndIsDeletedTrue(Long id, Long userId);

  long countByUserIdAndIsDeletedTrue(Long userId);

  @Query("SELECT v FROM VaultEntry v WHERE v.isDeleted = true AND v.deletedAt < :expiry")
  List<VaultEntry> findExpiredTrashEntries(@Param("expiry") LocalDateTime expiry);
}

