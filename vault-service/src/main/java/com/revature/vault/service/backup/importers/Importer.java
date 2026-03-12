package com.revature.vault.service.backup.importers;


import com.revature.vault.model.vault.VaultEntry;
import com.revature.vault.client.UserClient.UserVaultDetails;
import com.revature.vault.service.vault.EncryptionService;

import javax.crypto.SecretKey;
import java.util.List;

public interface Importer {
  List<VaultEntry> parse(String csvData, UserVaultDetails user, SecretKey key, EncryptionService encryptionService)
      throws Exception;

  String getSupportedSource();
}



