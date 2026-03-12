





package com.revature.vault.service.backup;

import com.revature.vault.dto.request.ThirdPartyImportRequest;
import com.revature.vault.dto.response.ImportResult;
import com.revature.vault.exception.ResourceNotFoundException;

import com.revature.vault.model.vault.VaultEntry;
import com.revature.vault.client.UserClient;
import com.revature.vault.client.UserClient.UserVaultDetails;
import com.revature.vault.service.vault.EncryptionService;
import com.revature.vault.service.vault.VaultService;
import com.revature.vault.service.backup.importers.Importer;
import com.revature.vault.service.backup.importers.ImporterFactory;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ThirdPartyImportService {

  private static final Logger logger = LoggerFactory.getLogger(ThirdPartyImportService.class);

  private final UserClient userClient;
  private final EncryptionService encryptionService;
  private final VaultService vaultService;
  private final ImporterFactory importerFactory;

  @Transactional
  public ImportResult importFromThirdParty(String username, ThirdPartyImportRequest request) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    // Derive key using the same method as VaultService (PBKDF2 with master password
    // hash and salt)
    SecretKey key = encryptionService.deriveKey(user.getMasterPasswordHash(), user.getSalt());

    try {
      Importer importer = importerFactory.getImporter(request.getSource());
      List<VaultEntry> entries = importer.parse(request.getData(), user, key, encryptionService);

      if (!entries.isEmpty()) {
        vaultService.bulkInsert(user.getUsername(), entries);
      }

      return ImportResult.builder()
          .totalProcessed(entries.size())
          .successCount(entries.size())
          .failCount(0)
          .message(request.getSource() + " import completed")
          .build();

    } catch (IllegalArgumentException e) {
      return ImportResult.builder()
          .totalProcessed(0).successCount(0).failCount(0)
          .message(e.getMessage())
          .build();
    } catch (Exception e) {
      logger.error("Failed to parse CSV for source {}: {}", request.getSource(), e.getMessage());
      return ImportResult.builder()
          .totalProcessed(0).successCount(0).failCount(1)
          .message(request.getSource() + " CSV parsing error")
          .build();
    }
  }

  @Transactional(readOnly = true)
  public List<String> getSupportedFormats() {
    return importerFactory.getSupportedFormats();
  }
}



