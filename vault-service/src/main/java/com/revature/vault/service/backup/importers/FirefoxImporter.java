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
public class FirefoxImporter implements Importer {

  @Override
  public String getSupportedSource() {
    return "FIREFOX";
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

        // Better Regex for CSV parsing (SonarQube fix: S5998 & S5850)
        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        for (int i = 0; i < parts.length; i++) {
          parts[i] = parts[i].replaceAll("^\"|\"$", "");
        }

        VaultEntry entry = VaultEntry.builder()
            .userId(user.getId())
            .websiteUrl(parts.length > 0 ? parts[0].trim() : "")
            .username(parts.length > 1 ? parts[1].trim() : "")
            .password(parts.length > 2 ? encryptionService.encrypt(parts[2].trim(), key) : "")
            .title(parts.length > 0 ? parts[0].trim() : "Firefox Import")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        entries.add(entry);
      }
    }
    return entries;
  }
}



