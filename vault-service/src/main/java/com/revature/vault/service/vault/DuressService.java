package com.revature.vault.service.vault;

import com.revature.vault.client.UserClient.UserVaultDetails;
import com.revature.vault.model.vault.VaultEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class DuressService {

  private static final SecureRandom RANDOM = new SecureRandom();

  private static final String[] DUMMY_TITLES = {
      "Email", "Social Media", "Banking", "Shopping", "Streaming",
      "Cloud Storage", "Gaming", "News", "Travel", "Fitness"
  };
  private static final String[] DUMMY_URLS = {
      "https://mail.example.com", "https://social.example.com",
      "https://bank.example.com", "https://shop.example.com",
      "https://stream.example.com", "https://cloud.example.com"
  };

  public List<VaultEntry> generateDummyVault(UserVaultDetails user) {
    List<VaultEntry> dummyEntries = new ArrayList<>();

    for (int i = 0; i < 5; i++) {
      VaultEntry entry = VaultEntry.builder()
          .id((long) -(i + 1))
          .userId(user.getId())
          .title(DUMMY_TITLES[RANDOM.nextInt(DUMMY_TITLES.length)])
          .username("user" + RANDOM.nextInt(1000) + "@example.com")
          .password("***encrypted***")
          .websiteUrl(DUMMY_URLS[RANDOM.nextInt(DUMMY_URLS.length)])
          .notes("")
          .createdAt(LocalDateTime.now().minusDays(RANDOM.nextInt(365)))
          .updatedAt(LocalDateTime.now().minusDays(RANDOM.nextInt(30)))
          .build();
      dummyEntries.add(entry);
    }
    return dummyEntries;
  }
}
