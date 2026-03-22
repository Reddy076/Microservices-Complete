package com.revature.security.service;

import com.revature.security.client.UserClient;
import com.revature.security.client.VaultAnalysisClient;
import com.revature.security.client.VaultAnalysisClient.DecryptedVaultEntryDTO;
import com.revature.security.dto.response.SecurityAuditResponse;
import com.revature.security.dto.response.SecurityAuditResponse.VaultEntrySummary;
import com.revature.security.model.PasswordAnalysis;
import com.revature.security.model.SecurityAlert.AlertType;
import com.revature.security.model.SecurityAlert.Severity;
import com.revature.security.repository.PasswordAnalysisRepository;
import com.revature.security.repository.SecurityAlertRepository;
import com.revature.security.util.PasswordStrengthCalculator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecurityAuditService {

  private static final Logger logger = LoggerFactory.getLogger(SecurityAuditService.class);
  private static final int OLD_PASSWORD_DAYS = 90;

  private final UserClient userClient;
  private final VaultAnalysisClient vaultAnalysisClient;
  private final PasswordStrengthCalculator passwordStrengthCalculator;
  private final PasswordAnalysisRepository passwordAnalysisRepository;
  private final SecurityAlertService securityAlertService;
  private final SecurityAlertRepository securityAlertRepository;


  @Transactional
  public void analyzeVault(Long userId) {
    try {
      List<DecryptedVaultEntryDTO> allEntries = vaultAnalysisClient.getDecryptedEntries(userId);
      if (allEntries == null) allEntries = new ArrayList<>();

      Map<String, List<DecryptedVaultEntryDTO>> passwordGroups = new HashMap<>();
      for (DecryptedVaultEntryDTO entry : allEntries) {
          String pwd = entry.getDecryptedPassword();
          if (pwd != null && !pwd.isBlank() && !pwd.equals("******")) {
              passwordGroups.computeIfAbsent(pwd, k -> new ArrayList<>()).add(entry);
          }
      }

      for (DecryptedVaultEntryDTO entry : allEntries) {
          String password = entry.getDecryptedPassword() != null ? entry.getDecryptedPassword() : "";
          int score = passwordStrengthCalculator.calculateScore(password);
          List<String> issues = generateIssues(password, score);

          boolean isReused = false;
          int reuseCount = 0;

          if (!password.isBlank() && !password.equals("******")) {
              List<DecryptedVaultEntryDTO> group = passwordGroups.get(password);
              if (group != null && group.size() > 1) {
                  isReused = true;
                  reuseCount = group.size() - 1;
              }
          }

          if (isReused) {
              issues.add("Password reused " + reuseCount + " times");
          }

          PasswordAnalysis analysis = passwordAnalysisRepository.findByVaultEntryId(entry.getEntryId())
              .orElse(PasswordAnalysis.builder().vaultEntryId(entry.getEntryId()).build());

          analysis.setStrengthScore(score);
          analysis.setReused(isReused);
          analysis.setIssues(issues);
          analysis.setLastAnalyzed(LocalDateTime.now());

          passwordAnalysisRepository.save(analysis);
          logger.debug("Analyzed entry {}: score={}, reused={}", entry.getEntryId(), score, isReused);
      }

    } catch (Exception e) {
      logger.error("Failed to analyze vault for user {}: {}", userId, e.getMessage());
    }
  }

  @Transactional
  public SecurityAuditResponse generateAuditReport(String username) {
    Long userId = userClient.getUserDetailsByUsername(username).getId();

    List<DecryptedVaultEntryDTO> allEntries = vaultAnalysisClient.getDecryptedEntries(userId);
    if (allEntries == null) allEntries = new ArrayList<>();
    syncMissingAnalyses(userId, allEntries);

    List<PasswordAnalysis> analyses = passwordAnalysisRepository.findByVaultEntryIdIn(
        allEntries.stream().map(DecryptedVaultEntryDTO::getEntryId).collect(Collectors.toList())
    );

    Map<Long, DecryptedVaultEntryDTO> entryMap = allEntries.stream()
        .collect(Collectors.toMap(DecryptedVaultEntryDTO::getEntryId, e -> e));

    List<VaultEntrySummary> weak = new ArrayList<>();
    List<VaultEntrySummary> reused = new ArrayList<>();
    List<VaultEntrySummary> old = new ArrayList<>();

    LocalDateTime cutoff = LocalDateTime.now().minusDays(OLD_PASSWORD_DAYS);

    for (PasswordAnalysis analysis : analyses) {
      DecryptedVaultEntryDTO entry = entryMap.get(analysis.getVaultEntryId());
      if (entry == null) continue;

      if (analysis.getStrengthScore() < 60) {
        weak.add(mapToSummary(entry, "Weak password (score: " + analysis.getStrengthScore() + "/100)"));
      }

      if (analysis.isReused()) {
        reused.add(mapToSummary(entry, "Password is reused"));
      }

      if (entry.getUpdatedAt() != null && entry.getUpdatedAt().isBefore(cutoff)) {
        old.add(mapToSummary(entry, "Password not updated in over " + OLD_PASSWORD_DAYS + " days"));
      }
    }

    int total = analyses.size();
    int securityScore = calculateSecurityScore(total, weak.size(), reused.size(), old.size());
    List<String> recommendations = generateRecommendations(weak.size(), reused.size(), old.size());

    SecurityAuditResponse report = SecurityAuditResponse.builder()
        .totalEntries(total)
        .weakCount(weak.size())
        .reusedCount(reused.size())
        .oldCount(old.size())
        .securityScore(securityScore)
        .recommendations(recommendations)
        .weakPasswords(weak)
        .reusedPasswords(reused)
        .oldPasswords(old)
        .build();

    // Refresh vault-quality security alerts
    try {
      refreshVaultAlerts(username, userId, weak, reused, old);
    } catch (Exception e) {
      logger.warn("Could not refresh vault alerts for {}: {}", username, e.getMessage());
    }

    return report;
  }

  private void refreshVaultAlerts(String username, Long userId,
      List<VaultEntrySummary> weak, List<VaultEntrySummary> reused, List<VaultEntrySummary> old) {
    List<AlertType> vaultAlertTypes = List.of(AlertType.WEAK_PASSWORD, AlertType.REUSED_PASSWORD, AlertType.OLD_PASSWORD);

    // Check if vault-quality alerts already exist (determines whether to send notifications)
    boolean hasExisting = securityAlertRepository.findByUserIdOrderByCreatedAtDesc(userId)
        .stream().anyMatch(a -> vaultAlertTypes.contains(a.getAlertType()));

    // Delete stale vault alerts so counts stay accurate
    securityAlertRepository.deleteByUserIdAndAlertTypeIn(userId, vaultAlertTypes);

    // Build a single flat list of alerts to create
    record AlertEntry(AlertType type, String title, String message, Severity severity) {}

    List<AlertEntry> toCreate = new ArrayList<>();
    for (VaultEntrySummary e : weak) {
      toCreate.add(new AlertEntry(AlertType.WEAK_PASSWORD, "Weak Password Detected",
          "'" + e.getTitle() + "' has a weak password. Use a stronger password with mixed characters.",
          Severity.HIGH));
    }
    for (VaultEntrySummary e : reused) {
      toCreate.add(new AlertEntry(AlertType.REUSED_PASSWORD, "Reused Password Detected",
          "'" + e.getTitle() + "' shares a password with another account. Use unique passwords.",
          Severity.MEDIUM));
    }
    for (VaultEntrySummary e : old) {
      toCreate.add(new AlertEntry(AlertType.OLD_PASSWORD, "Old Password Detected",
          "'" + e.getTitle() + "' password has not been updated in over 90 days.",
          Severity.HIGH));
    }

    for (AlertEntry ae : toCreate) {
      if (hasExisting) {
        // Silent refresh — no notification
        securityAlertRepository.save(buildAlert(userId, ae.type(), ae.title(), ae.message(), ae.severity()));
      } else {
        // First detection — notify the user
        securityAlertService.createAlert(username, ae.type(), ae.title(), ae.message(), ae.severity());
      }
    }
  }

  private com.revature.security.model.SecurityAlert buildAlert(Long userId, AlertType type,
      String title, String message, Severity severity) {
    return com.revature.security.model.SecurityAlert.builder()
        .userId(userId)
        .alertType(type)
        .title(title)
        .message(message)
        .severity(severity)
        .isRead(false)
        .createdAt(LocalDateTime.now())
        .build();
  }


  @Transactional(readOnly = true)
  public List<VaultEntrySummary> getWeakPasswords(String username) {
    return generateAuditReport(username).getWeakPasswords();
  }

  @Transactional(readOnly = true)
  public List<VaultEntrySummary> getReusedPasswords(String username) {
    return generateAuditReport(username).getReusedPasswords();
  }

  @Transactional(readOnly = true)
  public List<VaultEntrySummary> getOldPasswords(String username) {
    return generateAuditReport(username).getOldPasswords();
  }

  private VaultEntrySummary mapToSummary(DecryptedVaultEntryDTO entry, String issue) {
    return VaultEntrySummary.builder()
        .id(entry.getEntryId())
        .title(entry.getTitle())
        .websiteUrl(entry.getWebsiteUrl())
        .issue(issue)
        .build();
  }

  private int calculateSecurityScore(int total, int weak, int reused, int old) {
    if (total == 0) return 100;
    int issues = weak + reused + old;
    double ratio = 1.0 - ((double) issues / (total * 3));
    return Math.max(0, Math.min(100, (int) (ratio * 100)));
  }

  private List<String> generateIssues(String password, int score) {
    List<String> issues = new ArrayList<>();
    if (password.length() < 8) issues.add("Too short");
    if (score < 60) issues.add("Weak complexity");
    return issues;
  }

  private List<String> generateRecommendations(int weak, int reused, int old) {
    List<String> recommendations = new ArrayList<>();
    if (weak > 0) recommendations.add("Update " + weak + " weak password(s)");
    if (reused > 0) recommendations.add("Change " + reused + " reused password(s)");
    if (old > 0) recommendations.add("Rotate " + old + " old password(s)");
    if (recommendations.isEmpty()) recommendations.add("Your vault security is excellent!");
    return recommendations;
  }

  private void syncMissingAnalyses(Long userId, List<DecryptedVaultEntryDTO> allEntries) {
    List<Long> entryIds = allEntries.stream().map(DecryptedVaultEntryDTO::getEntryId).collect(Collectors.toList());
    if (entryIds.isEmpty()) return;
    
    List<PasswordAnalysis> existingAnalyses = passwordAnalysisRepository.findByVaultEntryIdIn(entryIds);
    Set<Long> analyzedEntryIds = existingAnalyses.stream()
        .map(PasswordAnalysis::getVaultEntryId)
        .collect(Collectors.toSet());

    boolean needSync = false;
    for (Long id : entryIds) {
      if (!analyzedEntryIds.contains(id)) {
        needSync = true;
        break;
      }
    }

    if (needSync) {
      logger.info("Found missing password analysis data for user {}. Running full re-scan.", userId);
      analyzeVault(userId);
    }
  }
}



