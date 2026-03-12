package com.revature.vault.service.vault;

import com.revature.vault.dto.response.SnapshotResponse;
import com.revature.vault.exception.ResourceNotFoundException;
import com.revature.vault.client.UserClient.UserVaultDetails;
import com.revature.vault.model.vault.VaultEntry;
import com.revature.vault.model.vault.VaultSnapshot;

import com.revature.vault.client.UserClient;
import com.revature.vault.repository.VaultEntryRepository;
import com.revature.vault.repository.VaultSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VaultSnapshotService {

  private static final Logger logger = LoggerFactory.getLogger(VaultSnapshotService.class);

  private final VaultSnapshotRepository vaultSnapshotRepository;
  private final VaultEntryRepository vaultEntryRepository;
  private final UserClient userClient;

  @Transactional
  public void createSnapshot(VaultEntry entry) {
    VaultSnapshot snapshot = VaultSnapshot.builder()
        .vaultEntry(entry)
        .password(entry.getPassword())
        .changedAt(LocalDateTime.now())
        .build();

    vaultSnapshotRepository.save(snapshot);
    logger.info("Created password snapshot for vault entry {}", entry.getId());
  }

  @Transactional(readOnly = true)
  public List<SnapshotResponse> getHistory(String username, Long entryId) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    vaultEntryRepository.findByIdAndUserId(entryId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Vault entry not found"));

    List<VaultSnapshot> snapshots = vaultSnapshotRepository
        .findByVaultEntryIdOrderByChangedAtDesc(entryId);

    return snapshots.stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<SnapshotResponse> getAllSnapshots(String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    List<VaultSnapshot> snapshots = vaultSnapshotRepository
        .findByVaultEntryUserIdOrderByChangedAtDesc(user.getId());

    return snapshots.stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public void restoreSnapshot(String username, Long snapshotId) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    VaultSnapshot snapshot = vaultSnapshotRepository.findById(snapshotId)
        .orElseThrow(() -> new ResourceNotFoundException("Snapshot not found"));

    VaultEntry entry = snapshot.getVaultEntry();
    if (!entry.getUserId().equals(user.getId())) {
      throw new com.revature.vault.exception.AuthenticationException(
          "Not authorized to restore this snapshot");
    }

    createSnapshot(entry);

    entry.setPassword(snapshot.getPassword());
    entry.setUpdatedAt(LocalDateTime.now());
    vaultEntryRepository.save(entry);

    logger.info("Restored password snapshot {} for vault entry {}", snapshotId, entry.getId());
  }

  private SnapshotResponse mapToResponse(VaultSnapshot snapshot) {
    return SnapshotResponse.builder()
        .id(snapshot.getId())
        .password("******")
        .entryTitle(snapshot.getVaultEntry() != null ? snapshot.getVaultEntry().getTitle() : "Unknown Entry")
        .changedAt(snapshot.getChangedAt())
        .build();
  }
}




