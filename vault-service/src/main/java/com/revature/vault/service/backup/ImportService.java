package com.revature.vault.service.backup;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.vault.dto.request.ImportRequest;
import com.revature.vault.dto.response.ImportResult;

import com.revature.vault.model.vault.Category;
import com.revature.vault.model.vault.Folder;
import com.revature.vault.model.vault.VaultEntry;
import com.revature.vault.repository.CategoryRepository;
import com.revature.vault.repository.FolderRepository;
import com.revature.vault.client.UserClient;
import com.revature.vault.client.UserClient.UserVaultDetails;
import com.revature.vault.repository.VaultEntryRepository;
import com.revature.vault.service.vault.EncryptionService;
import com.revature.vault.service.vault.VaultService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImportService {

  private static final Logger logger = LoggerFactory.getLogger(ImportService.class);

  private final VaultEntryRepository vaultEntryRepository;
  private final UserClient userClient;
  private final CategoryRepository categoryRepository;
  private final FolderRepository folderRepository;
  private final EncryptionService encryptionService;
  private final VaultService vaultService;

  @Transactional
  public ImportResult importVault(String username, ImportRequest request) {
    logger.info("Import request received for user: {} with format: {}", username, request.getFormat());
    logger.debug("Import data length: {}", request.getData() != null ? request.getData().length() : 0);

    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    // Derive key using the same method as VaultService (PBKDF2 with master password
    // hash and salt)
    SecretKey key = encryptionService.deriveKey(user.getMasterPasswordHash(), user.getSalt());

    if (request.getFormat() == null || request.getFormat().isEmpty()) {
      logger.error("Format is null or empty");
      return ImportResult.builder()
          .totalProcessed(0).successCount(0).failCount(0)
          .message("Format is required")
          .build();
    }

    if (request.getData() == null || request.getData().isEmpty()) {
      logger.error("Data is null or empty");
      return ImportResult.builder()
          .totalProcessed(0).successCount(0).failCount(0)
          .message("Data is required")
          .build();
    }

    // Detect encrypted export: ExportService stores files as "salt:ciphertext"
    String data = request.getData().trim();
    if (looksEncrypted(data)) {
      if (request.getPassword() == null || request.getPassword().isBlank()) {
        return ImportResult.builder()
            .totalProcessed(0).successCount(0).failCount(0)
            .message("This file is encrypted. Please provide the decryption password.")
            .build();
      }
      try {
        int colonIdx = data.indexOf(':');
        String salt = data.substring(0, colonIdx);
        String ciphertext = data.substring(colonIdx + 1);
        data = encryptionService.decryptForImport(ciphertext, request.getPassword(), salt);
        logger.info("Encrypted import file decrypted successfully for user: {}", username);
      } catch (Exception e) {
        logger.warn("Failed to decrypt import file for user {}: {}", username, e.getMessage());
        return ImportResult.builder()
            .totalProcessed(0).successCount(0).failCount(0)
            .message("Decryption failed. The password may be incorrect.")
            .build();
      }
    }

    switch (request.getFormat().toUpperCase()) {
      case "JSON":
        return importJson(user, data, key);
      case "CSV":
        return importCsv(user, data, key);
      default:
        return ImportResult.builder()
            .totalProcessed(0).successCount(0).failCount(0)
            .message("Unsupported format: " + request.getFormat())
            .build();
    }
  }

  /**
   * Heuristic: encrypted exports start with a UUID-style salt followed by ':'.
   * They are never valid JSON so we can safely distinguish them.
   */
  private boolean looksEncrypted(String data) {
    // Encrypted format: "<uuid-salt>:<base64-ciphertext>"
    // A UUID salt is 36 chars; check that the first ':' appears before position 50
    // and the content is NOT valid JSON (doesn't start with '[' or '{')
    if (data.startsWith("[") || data.startsWith("{"))
      return false;
    int colonIdx = data.indexOf(':');
    return colonIdx > 0 && colonIdx < 50;
  }

  @Transactional(readOnly = true)
  public ImportResult validateImport(String username, ImportRequest request) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    // Derive key using the same method as VaultService (PBKDF2 with master password
    // hash and salt)
    SecretKey key = encryptionService.deriveKey(user.getMasterPasswordHash(), user.getSalt());

    switch (request.getFormat().toUpperCase()) {
      case "JSON":
        return validateJson(user, request.getData(), key);
      case "CSV":
        return validateCsv(user, request.getData(), key);
      default:
        return ImportResult.builder()
            .totalProcessed(0).successCount(0).failCount(0)
            .message("Unsupported format: " + request.getFormat())
            .build();
    }
  }

  private ImportResult validateJson(UserVaultDetails user, String data, SecretKey key) {
    int success = 0;
    int fail = 0;
    try {
      ObjectMapper mapper = new ObjectMapper();
      List<Map<String, Object>> entries = mapper.readValue(data, new TypeReference<>() {
      });
      for (Map<String, Object> entryMap : entries) {
        try {
          if (getString(entryMap, "title").isEmpty() && getString(entryMap, "websiteUrl").isEmpty()) {
            fail++;
            continue;
          }
          success++;
        } catch (Exception e) {
          fail++;
        }
      }
    } catch (Exception e) {
      return ImportResult.builder().totalProcessed(0).successCount(0).failCount(1).message("Invalid JSON format")
          .build();
    }
    return ImportResult.builder().totalProcessed(success + fail).successCount(success).failCount(fail)
        .message("Validation passed").build();
  }

  private ImportResult validateCsv(UserVaultDetails user, String data, SecretKey key) {
    int success = 0, fail = 0;
    try (BufferedReader reader = new BufferedReader(new StringReader(data))) {
      String headerLine = reader.readLine();
      if (headerLine == null)
        return ImportResult.builder().totalProcessed(0).successCount(0).failCount(0).message("Empty CSV").build();
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().isEmpty()) {
          continue;
        }
        success++;
      }
    } catch (Exception e) {
      return ImportResult.builder().totalProcessed(0).successCount(0).failCount(1).message("CSV parsing error").build();
    }
    return ImportResult.builder().totalProcessed(success + fail).successCount(success).failCount(fail)
        .message("Validation passed").build();
  }

  private ImportResult importJson(UserVaultDetails user, String data, SecretKey key) {
    int success = 0, fail = 0;
    java.util.List<VaultEntry> entriesToSave = new java.util.ArrayList<>();

    // Cache for categories and folders to avoid duplicate lookups
    Map<String, Category> categoryCache = new HashMap<>();
    Map<String, Folder> folderCache = new HashMap<>();

    try {
      ObjectMapper mapper = new ObjectMapper();
      List<Map<String, Object>> entries = mapper.readValue(data, new TypeReference<>() {
      });
      for (Map<String, Object> entryMap : entries) {
        try {
          String password = entryMap.get("password") != null ? entryMap.get("password").toString() : "";

          String notes = getString(entryMap, "notes");

          // Build the entry
          VaultEntry.VaultEntryBuilder entryBuilder = VaultEntry.builder()
              .userId(user.getId())
              .title(getString(entryMap, "title"))
              .username(encryptionService.encrypt(getString(entryMap, "username"), key))
              .password(encryptionService.encrypt(password, key))
              .websiteUrl(getString(entryMap, "websiteUrl"))
              .notes(notes.isEmpty() ? null : encryptionService.encrypt(notes, key))
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now());

          // Handle category
          if (entryMap.containsKey("category")) {
            Map<String, Object> categoryMap = (Map<String, Object>) entryMap.get("category");
            String categoryName = categoryMap != null ? (String) categoryMap.get("name") : null;
            if (categoryName != null && !categoryName.isEmpty()) {
              Category category = categoryCache.computeIfAbsent(categoryName, name -> {
                // Try to find existing category or create new one
                return categoryRepository.findByUserIdAndName(user.getId(), name)
                    .orElseGet(() -> {
                      Category newCategory = Category.builder()
                          .name(name)
                          .userId(user.getId())
                          .icon(getString(categoryMap, "icon"))
                          .isDefault(false)
                          .createdAt(LocalDateTime.now())
                          .build();
                      return categoryRepository.save(newCategory);
                    });
              });
              entryBuilder.category(category);
            }
          }

          // Handle folder - since there's no findByUserAndName, we'll search in the list
          if (entryMap.containsKey("folder")) {
            Map<String, Object> folderMap = (Map<String, Object>) entryMap.get("folder");
            String folderName = folderMap != null ? (String) folderMap.get("name") : null;
            if (folderName != null && !folderName.isEmpty()) {
              Folder folder = folderCache.computeIfAbsent(folderName, name -> {
                // Search for existing folder by user and name
                List<Folder> userFolders = folderRepository.findByUserId(user.getId());
                return userFolders.stream()
                    .filter(f -> f.getName().equals(name))
                    .findFirst()
                    .orElseGet(() -> {
                      Folder newFolder = Folder.builder()
                          .name(name)
                          .userId(user.getId())
                          .createdAt(LocalDateTime.now())
                          .updatedAt(LocalDateTime.now())
                          .build();
                      return folderRepository.save(newFolder);
                    });
              });
              entryBuilder.folder(folder);
            }
          }

          VaultEntry entry = entryBuilder.build();
          entriesToSave.add(entry);
          success++;
        } catch (Exception e) {
          logger.warn("Failed to import entry: {}", e.getMessage());
          fail++;
        }
      }
      vaultService.bulkInsert(user.getUsername(), entriesToSave);
    } catch (Exception e) {
      logger.error("Failed to parse JSON import: {}", e.getMessage());
      return ImportResult.builder()
          .totalProcessed(0).successCount(0).failCount(1)
          .message("Invalid JSON format")
          .build();
    }
    return ImportResult.builder()
        .totalProcessed(success + fail).successCount(success).failCount(fail)
        .message("Import completed")
        .build();
  }

  private ImportResult importCsv(UserVaultDetails user, String data, SecretKey key) {
    int success = 0, fail = 0;
    java.util.List<VaultEntry> entriesToSave = new java.util.ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new StringReader(data))) {
      String headerLine = reader.readLine();
      if (headerLine == null) {
        return ImportResult.builder()
            .totalProcessed(0).successCount(0).failCount(0)
            .message("Empty CSV").build();
      }
      String line;
      while ((line = reader.readLine()) != null) {
        try {
          String[] parts = line.split(",", -1);
          String title = parts.length > 0 ? parts[0].trim() : "";
          String rawUsername = parts.length > 1 ? parts[1].trim() : "";
          String rawPassword = parts.length > 2 ? parts[2].trim() : "";
          String rawWebsite = parts.length > 3 ? parts[3].trim() : "";
          String rawNotes = parts.length > 4 ? parts[4].trim() : "";

          VaultEntry entry = VaultEntry.builder()
              .userId(user.getId())
              .title(title)
              .username(encryptionService.encrypt(rawUsername, key))
              .password(encryptionService.encrypt(rawPassword, key))
              .websiteUrl(rawWebsite)
              .notes(rawNotes.isEmpty() ? null : encryptionService.encrypt(rawNotes, key))
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .build();
          entriesToSave.add(entry);
          success++;
        } catch (Exception e) {
          logger.warn("Failed to import CSV line: {}", e.getMessage());
          fail++;
        }
      }
      vaultService.bulkInsert(user.getUsername(), entriesToSave);
    } catch (Exception e) {
      return ImportResult.builder()
          .totalProcessed(0).successCount(0).failCount(1)
          .message("CSV parsing error").build();
    }
    return ImportResult.builder()
        .totalProcessed(success + fail).successCount(success).failCount(fail)
        .message("Import completed").build();
  }

  private String getString(Map<String, Object> map, String key) {
    Object value = map.get(key);
    return value != null ? value.toString() : "";
  }
}




