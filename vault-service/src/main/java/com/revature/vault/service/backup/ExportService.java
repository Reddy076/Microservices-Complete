package com.revature.vault.service.backup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.revature.vault.dto.response.ExportResponse;
import com.revature.vault.exception.ResourceNotFoundException;
import com.revature.vault.model.backup.BackupExport;
import com.revature.vault.model.backup.BackupExport.ExportFormat;


import com.revature.vault.model.vault.VaultEntry;
import com.revature.vault.repository.BackupExportRepository;
import com.revature.vault.client.UserClient;
import com.revature.vault.client.UserClient.UserVaultDetails;
import com.revature.vault.repository.VaultEntryRepository;
import com.revature.vault.client.SecurityClient;
import com.revature.vault.service.vault.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportService {

  private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

  private final VaultEntryRepository vaultEntryRepository;
  private final UserClient userClient;
  private final BackupExportRepository backupExportRepository;
  private final EncryptionService encryptionService;
  private final SecurityClient securityClient;

  @Transactional
  public ExportResponse exportVault(String username, String format) {
    return exportVault(username, format, null);
  }

  @Transactional
  public ExportResponse exportVault(String username, String format, String encryptionPassword) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    List<VaultEntry> entries = vaultEntryRepository.findByUserIdAndIsDeletedFalse(user.getId());
    String normalizedFormat = normalizeFormat(format);
    ExportFormat exportFormat = resolveExportFormat(normalizedFormat);

    // Derive the encryption key for decrypting passwords
    SecretKey key = encryptionService.deriveKey(user.getMasterPasswordHash(), user.getSalt());

    String data;
    switch (exportFormat) {
      case JSON:
        data = exportAsJson(entries, key);
        break;
      case CSV:
        data = exportAsCsv(entries, key);
        break;
      default:
        data = exportAsJson(entries, key);
    }

    boolean encrypted = false;
    if (encryptionPassword != null && !encryptionPassword.isBlank()) {
      String salt = UUID.randomUUID().toString();
      data = salt + ":" + encryptionService.encryptForExport(data, encryptionPassword, salt);
      encrypted = true;
    }

    BackupExport export = BackupExport.builder()
        .userId(user.getId())
        .exportFormat(exportFormat)
        .fileName("vault_export_" + System.currentTimeMillis() + "." + exportFormat.name().toLowerCase())
        .entryCount(entries.size())
        .isEncrypted(encrypted)
        .createdAt(LocalDateTime.now())
        .build();
    backupExportRepository.save(export);

    securityClient.logAction(username, "VAULT_EXPORTED",
        String.format("Exported %d entries in %s format (encrypted: %s)",
            entries.size(), normalizedFormat, encrypted));

    logger.info("Vault exported for user: {} ({} entries, encrypted: {})",
        username, entries.size(), encrypted);

    return ExportResponse.builder()
        .fileName(export.getFileName())
        .format(normalizedFormat)
        .entryCount(entries.size())
        .encrypted(encrypted)
        .data(data)
        .exportedAt(export.getCreatedAt())
        .build();
  }

  private String exportAsJson(List<VaultEntry> entries, SecretKey key) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
      List<Map<String, Object>> exportData = entries.stream()
          .map(entry -> entryToMap(entry, key))
          .collect(Collectors.toList());
      return mapper.writeValueAsString(exportData);
    } catch (Exception e) {
      logger.error("Failed to export as JSON: {}", e.getMessage());
      throw new RuntimeException("Export failed", e);
    }
  }

  private String exportAsCsv(List<VaultEntry> entries, SecretKey key) {
    StringBuilder sb = new StringBuilder();
    sb.append("title,username,password,website_url,notes,is_favorite\n");
    for (VaultEntry entry : entries) {
      String decryptedPassword = decryptField(entry.getPassword(), key);
      String decryptedUsername = decryptField(entry.getUsername(), key);
      String decryptedNotes = decryptField(entry.getNotes(), key);
      sb.append(escapeCsv(entry.getTitle())).append(",");
      sb.append(escapeCsv(decryptedUsername)).append(",");
      sb.append(escapeCsv(decryptedPassword)).append(",");
      sb.append(escapeCsv(entry.getWebsiteUrl())).append(",");
      sb.append(escapeCsv(decryptedNotes)).append(",");
      sb.append(entry.getIsFavorite()).append("\n");
    }
    return sb.toString();
  }

  private Map<String, Object> entryToMap(VaultEntry entry, SecretKey key) {
    Map<String, Object> map = new HashMap<>();
    map.put("title", entry.getTitle());
    map.put("username", decryptField(entry.getUsername(), key));
    map.put("password", decryptField(entry.getPassword(), key));
    map.put("websiteUrl", entry.getWebsiteUrl());
    map.put("notes", decryptField(entry.getNotes(), key));
    map.put("isFavorite", entry.getIsFavorite());
    map.put("createdAt", entry.getCreatedAt() != null ? entry.getCreatedAt().toString() : null);

    // Include category information
    if (entry.getCategory() != null) {
      Map<String, Object> categoryMap = new HashMap<>();
      categoryMap.put("id", entry.getCategory().getId());
      categoryMap.put("name", entry.getCategory().getName());
      map.put("category", categoryMap);
    }

    // Include folder information
    if (entry.getFolder() != null) {
      Map<String, Object> folderMap = new HashMap<>();
      folderMap.put("id", entry.getFolder().getId());
      folderMap.put("name", entry.getFolder().getName());
      map.put("folder", folderMap);
    }

    return map;
  }

  private String decryptField(String encryptedValue, SecretKey key) {
    try {
      if (encryptedValue == null || encryptedValue.isEmpty()) {
        return encryptedValue == null ? "" : encryptedValue;
      }
      return encryptionService.decrypt(encryptedValue, key);
    } catch (Exception e) {
      logger.warn("Failed to decrypt field for export: {}", e.getMessage());
      return "";
    }
  }

  private String escapeCsv(String value) {
    if (value == null)
      return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  @Transactional(readOnly = true)
  public ExportResponse previewExport(String username, String format) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    List<VaultEntry> entries = vaultEntryRepository.findByUserIdAndIsDeletedFalse(user.getId());

    List<VaultEntry> previewEntries = entries.stream().limit(3).collect(Collectors.toList());

    // Derive the encryption key for decrypting passwords
    SecretKey key = encryptionService.deriveKey(user.getMasterPasswordHash(), user.getSalt());

    String normalizedFormat = normalizeFormat(format);
    ExportFormat exportFormat = resolveExportFormat(normalizedFormat);
    String data = exportFormat == ExportFormat.CSV ? exportAsCsv(previewEntries, key)
        : exportAsJson(previewEntries, key);

    return ExportResponse.builder()
        .fileName("preview." + exportFormat.name().toLowerCase())
        .format(normalizedFormat)
        .entryCount(entries.size())
        .encrypted(false)
        .data(data)
        .exportedAt(LocalDateTime.now())
        .build();
  }

  private String normalizeFormat(String format) {
    return format == null ? ExportFormat.JSON.name() : format.trim().toUpperCase();
  }

  private ExportFormat resolveExportFormat(String format) {
    return "ENCRYPTED".equals(format) ? ExportFormat.JSON : ExportFormat.valueOf(format);
  }
}



