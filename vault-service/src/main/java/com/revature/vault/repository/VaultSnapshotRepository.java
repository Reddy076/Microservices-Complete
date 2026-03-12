package com.revature.vault.repository;

import com.revature.vault.model.vault.VaultSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VaultSnapshotRepository extends JpaRepository<VaultSnapshot, Long> {

  List<VaultSnapshot> findByVaultEntryIdOrderByChangedAtDesc(Long vaultEntryId);

  List<VaultSnapshot> findByVaultEntryUserIdOrderByChangedAtDesc(Long userId);

  void deleteByVaultEntryId(Long vaultEntryId);
}

