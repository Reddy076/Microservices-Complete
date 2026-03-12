package com.revature.vault.service.backup.importers;


import com.revature.vault.model.vault.VaultEntry;
import com.revature.vault.client.UserClient.UserVaultDetails;
import com.revature.vault.service.vault.EncryptionService;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class ChromeImporter implements Importer {

  @Override
  public String getSupportedSource() {
    return "CHROME";
  }

  @Override
  public List<VaultEntry> parse(String csvData, UserVaultDetails user, SecretKey key, EncryptionService encryptionService)
      throws Exception {
    List<VaultEntry> entries = new ArrayList<>();

    try (BufferedReader reader = new BufferedReader(new StringReader(csvData))) {
      @SuppressWarnings("unused") // Ignore header row
      String header = reader.readLine();
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().isEmpty())
          continue;
        String[] parts = line.split(",", -1);
        VaultEntry entry = VaultEntry.builder()
            .userId(user.getId())
            .title(parts.length > 0 ? parts[0].trim() : "")
            .websiteUrl(parts.length > 1 ? parts[1].trim() : "")
            .username(parts.length > 2 ? parts[2].trim() : "")
            .password(parts.length > 3 ? encryptionService.encrypt(parts[3].trim(), key) : "")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        entries.add(entry);
      }
    }
    return entries;
  }
}



