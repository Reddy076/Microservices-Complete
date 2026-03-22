package com.revature.vault.service.vault;

import com.revature.vault.dto.response.TrashEntryResponse;
import com.revature.vault.exception.ResourceNotFoundException;
import com.revature.vault.client.UserClient.UserVaultDetails;
import com.revature.vault.model.vault.VaultEntry;
import com.revature.vault.client.UserClient;
import com.revature.vault.repository.VaultTrashRepository;
import com.revature.vault.repository.VaultSnapshotRepository;

import com.revature.vault.repository.SecureShareRepository;


import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VaultTrashService {

  private static final Logger logger = LoggerFactory.getLogger(VaultTrashService.class);
  private static final int TRASH_RETENTION_DAYS = 30;

  private final VaultTrashRepository vaultTrashRepository;
  private final UserClient userClient;
  private final VaultSnapshotRepository vaultSnapshotRepository;
  
  
  private final SecureShareRepository secureShareRepository;

  @Transactional(readOnly = true)
  public List<TrashEntryResponse> getTrashEntries(String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    List<VaultEntry> entries = vaultTrashRepository.findByUserIdAndIsDeletedTrue(user.getId());
    return entries.stream()
        .map(this::mapToTrashResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public long getTrashCount(String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    return vaultTrashRepository.countByUserIdAndIsDeletedTrue(user.getId());
  }

  @Transactional
  public TrashEntryResponse restoreEntry(String username, Long entryId) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    VaultEntry entry = vaultTrashRepository.findByIdAndUserIdAndIsDeletedTrue(entryId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Trashed entry not found"));

    entry.setIsDeleted(false);
    entry.setDeletedAt(null);
    vaultTrashRepository.save(entry);


    logger.info("Restored vault entry {} for user {}", entryId, username);
    return mapToTrashResponse(entry);
  }

  @Transactional
  public void restoreAll(String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    List<VaultEntry> trashedEntries = vaultTrashRepository.findByUserIdAndIsDeletedTrue(user.getId());

    trashedEntries.forEach(entry -> {
      entry.setIsDeleted(false);
      entry.setDeletedAt(null);
    });

    vaultTrashRepository.saveAll(trashedEntries);


    logger.info("Restored {} trashed entries for user {}", trashedEntries.size(), username);
  }

  @Transactional
  public void permanentDelete(String username, Long entryId) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    VaultEntry entry = vaultTrashRepository.findByIdAndUserIdAndIsDeletedTrue(entryId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Trashed entry not found"));

    secureShareRepository.deleteByVaultEntryId(entry.getId());
    vaultSnapshotRepository.deleteByVaultEntryId(entry.getId());

    vaultTrashRepository.delete(entry);

    logger.info("Permanently deleted vault entry {} for user {}", entryId, username);
  }

  @Transactional
  public void emptyTrash(String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    List<VaultEntry> trashedEntries = vaultTrashRepository.findByUserIdAndIsDeletedTrue(user.getId());

    trashedEntries.forEach(entry -> {
      secureShareRepository.deleteByVaultEntryId(entry.getId());
      vaultSnapshotRepository.deleteByVaultEntryId(entry.getId());
    });

    vaultTrashRepository.deleteAll(trashedEntries);

    logger.info("Emptied trash ({} entries) for user {}", trashedEntries.size(), username);
  }

  @Transactional
  public void cleanupExpired() {
    LocalDateTime expiry = LocalDateTime.now().minusDays(TRASH_RETENTION_DAYS);
    List<VaultEntry> expired = vaultTrashRepository.findExpiredTrashEntries(expiry);

    if (!expired.isEmpty()) {
      expired.forEach(entry -> {
        secureShareRepository.deleteByVaultEntryId(entry.getId());
        vaultSnapshotRepository.deleteByVaultEntryId(entry.getId());
      });
      vaultTrashRepository.deleteAll(expired);
      logger.info("Cleaned up {} expired trash entries", expired.size());
    }
  }

  private TrashEntryResponse mapToTrashResponse(VaultEntry entry) {
    LocalDateTime expiresAt = entry.getDeletedAt() != null
        ? entry.getDeletedAt().plusDays(TRASH_RETENTION_DAYS)
        : null;
    long daysRemaining = expiresAt != null
        ? Math.max(0, ChronoUnit.DAYS.between(LocalDateTime.now(), expiresAt))
        : 0;

    return TrashEntryResponse.builder()
        .id(entry.getId())
        .title(entry.getTitle())
        .websiteUrl(entry.getWebsiteUrl())
        .categoryName(entry.getCategory() != null ? entry.getCategory().getName() : null)
        .folderName(entry.getFolder() != null ? entry.getFolder().getName() : null)
        .deletedAt(entry.getDeletedAt())
        .expiresAt(expiresAt)
        .daysRemaining(daysRemaining)
        .build();
  }
}

