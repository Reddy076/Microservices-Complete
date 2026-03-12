package com.revature.security.dashboard;

import com.revature.security.client.VaultAnalysisClient;
import com.revature.security.client.VaultAnalysisClient.DecryptedVaultEntryDTO;
import com.revature.security.dto.response.PasswordAgeResponse;
import com.revature.security.dto.response.PasswordHealthMetricsResponse;
import com.revature.security.dto.response.ReusedPasswordResponse;
import com.revature.security.dto.response.SecurityScoreResponse;
import com.revature.security.dto.response.VaultEntryResponse;
import com.revature.security.util.PasswordStrengthCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SecurityMetricsCalculator {

    private static final int OLD_PASSWORD_THRESHOLD_DAYS = 90;
    private static final int ANCIENT_PASSWORD_THRESHOLD_DAYS = 180;
    private static final int AGING_PASSWORD_THRESHOLD_DAYS = 30;

    private final VaultAnalysisClient vaultAnalysisClient;
    private final PasswordStrengthCalculator passwordStrengthCalculator;

    public SecurityScoreResponse calculateSecurityScore(Long userId) {
        List<DecryptedVaultEntryDTO> entries = vaultAnalysisClient.getDecryptedEntries(userId);

        if (entries == null || entries.isEmpty()) {
            return SecurityScoreResponse.builder()
                    .overallScore(100)
                    .scoreLabel("Excellent")
                    .totalPasswords(0)
                    .recommendation("Add passwords to your vault to start tracking security metrics.")
                    .build();
        }

        int total = entries.size();
        int weakCount = 0;
        int fairCount = 0;
        int goodCount = 0;
        int strongCount = 0;

        Map<String, List<String>> decryptedPasswordGroups = new HashMap<>();

        for (DecryptedVaultEntryDTO entry : entries) {
            String decrypted = entry.getDecryptedPassword();
            if (decrypted == null) {
                decrypted = "";
            }
            int score = passwordStrengthCalculator.calculateScore(decrypted);
            String label = passwordStrengthCalculator.getStrengthLabel(score);

            if (label != null) {
                switch (label) {
                    case "Very Weak", "Weak" -> weakCount++;
                    case "Fair" -> fairCount++;
                    case "Good" -> goodCount++;
                    case "Strong" -> strongCount++;
                }
            }

            if (!decrypted.isBlank()) {
                decryptedPasswordGroups
                        .computeIfAbsent(decrypted, k -> new ArrayList<>())
                        .add(entry.getTitle());
            }
        }

        long reusedCount = decryptedPasswordGroups.values().stream()
                .filter(v -> v.size() > 1)
                .mapToLong(v -> v.size())
                .sum();

        long oldCount = entries.stream()
                .filter(e -> e.getUpdatedAt() != null &&
                        ChronoUnit.DAYS.between(e.getUpdatedAt(), LocalDateTime.now()) > OLD_PASSWORD_THRESHOLD_DAYS)
                .count();

        int overallScore = computeOverallScore(total, weakCount, (int) reusedCount, (int) oldCount);
        String label = getScoreLabel(overallScore);
        String recommendation = buildRecommendation(weakCount, (int) reusedCount, (int) oldCount);

        return SecurityScoreResponse.builder()
                .overallScore(overallScore)
                .scoreLabel(label)
                .totalPasswords(total)
                .strongPasswords(strongCount + goodCount)
                .fairPasswords(fairCount)
                .weakPasswords(weakCount)
                .reusedPasswords((int) reusedCount)
                .oldPasswords((int) oldCount)
                .recommendation(recommendation)
                .build();
    }

    public PasswordHealthMetricsResponse calculatePasswordHealth(Long userId) {
        List<DecryptedVaultEntryDTO> entries = vaultAnalysisClient.getDecryptedEntries(userId);
        if (entries == null) {
            entries = new ArrayList<>();
        }

        int total = entries.size();
        int strongCount = 0;
        int goodCount = 0;
        int fairCount = 0;
        int weakCount = 0;
        int veryWeakCount = 0;
        double totalScore = 0;

        Map<Long, Integer> scoreCache = new HashMap<>();
        for (DecryptedVaultEntryDTO entry : entries) {
            String decrypted = entry.getDecryptedPassword();
            if (decrypted == null) decrypted = "";
            int score = passwordStrengthCalculator.calculateScore(decrypted);
            scoreCache.put(entry.getEntryId(), score);
            totalScore += score;
            String strengthLabel = passwordStrengthCalculator.getStrengthLabel(score);
            if (strengthLabel != null) {
                switch (strengthLabel) {
                    case "Strong" -> strongCount++;
                    case "Good" -> goodCount++;
                    case "Fair" -> fairCount++;
                    case "Weak" -> weakCount++;
                    case "Very Weak" -> veryWeakCount++;
                }
            }
        }

        Map<String, List<DecryptedVaultEntryDTO>> byCategory = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getCategoryName() != null ? e.getCategoryName() : "Uncategorized"));

        List<PasswordHealthMetricsResponse.PasswordCategoryBreakdown> breakdowns = new ArrayList<>();

        for (Map.Entry<String, List<DecryptedVaultEntryDTO>> catEntry : byCategory.entrySet()) {
            List<DecryptedVaultEntryDTO> catEntries = catEntry.getValue();
            double catTotalScore = 0;
            int catWeakCount = 0;
            for (DecryptedVaultEntryDTO e : catEntries) {
                int score = scoreCache.getOrDefault(e.getEntryId(), 0);
                catTotalScore += score;
                if (score < 40) catWeakCount++;
            }
            breakdowns.add(PasswordHealthMetricsResponse.PasswordCategoryBreakdown.builder()
                    .categoryName(catEntry.getKey())
                    .count(catEntries.size())
                    .averageScore(catEntries.isEmpty() ? 0 : catTotalScore / catEntries.size())
                    .weakCount(catWeakCount)
                    .build());
        }

        return PasswordHealthMetricsResponse.builder()
                .totalPasswords(total)
                .strongCount(strongCount)
                .goodCount(goodCount)
                .fairCount(fairCount)
                .weakCount(weakCount)
                .veryWeakCount(veryWeakCount)
                .averageStrengthScore(total == 0 ? 0 : totalScore / total)
                .categoryBreakdowns(breakdowns)
                .build();
    }

    public ReusedPasswordResponse findReusedPasswords(Long userId) {
        List<DecryptedVaultEntryDTO> entries = vaultAnalysisClient.getDecryptedEntries(userId);
        if (entries == null) entries = new ArrayList<>();
        
        Map<String, List<DecryptedVaultEntryDTO>> byPassword = new HashMap<>();
        for (DecryptedVaultEntryDTO entry : entries) {
            String decrypted = entry.getDecryptedPassword();
            if (decrypted != null && !decrypted.isBlank()) {
                byPassword.computeIfAbsent(decrypted, k -> new ArrayList<>()).add(entry);
            }
        }

        List<ReusedPasswordResponse.ReusedPasswordGroup> groups = new ArrayList<>();
        int totalAffected = 0;

        for (Map.Entry<String, List<DecryptedVaultEntryDTO>> pw : byPassword.entrySet()) {
            List<DecryptedVaultEntryDTO> group = pw.getValue();
            if (group.size() > 1) {
                totalAffected += group.size();
                List<ReusedPasswordResponse.ReusedEntryInfo> infos = group.stream()
                        .map(e -> ReusedPasswordResponse.ReusedEntryInfo.builder()
                                .entryId(e.getEntryId())
                                .title(e.getTitle())
                                .websiteUrl(e.getWebsiteUrl())
                                .build())
                        .collect(Collectors.toList());

                groups.add(ReusedPasswordResponse.ReusedPasswordGroup.builder()
                        .reuseCount(group.size())
                        .entries(infos)
                        .build());
            }
        }

        return ReusedPasswordResponse.builder()
                .totalReusedGroups(groups.size())
                .totalAffectedEntries(totalAffected)
                .reusedGroups(groups)
                .build();
    }

    public PasswordAgeResponse calculatePasswordAge(Long userId) {
        List<DecryptedVaultEntryDTO> entries = vaultAnalysisClient.getDecryptedEntries(userId);
        if (entries == null) entries = new ArrayList<>();

        int fresh = 0;
        int aging = 0;
        int old = 0;
        int ancient = 0;
        double totalAgeDays = 0;

        LocalDateTime now = LocalDateTime.now();
        Map<String, Integer> buckets = new HashMap<>();
        buckets.put("< 30 days", 0);
        buckets.put("30-90 days", 0);
        buckets.put("90-180 days", 0);
        buckets.put("> 180 days", 0);

        for (DecryptedVaultEntryDTO entry : entries) {
            LocalDateTime lastChanged = entry.getUpdatedAt() != null ? entry.getUpdatedAt() : entry.getCreatedAt();
            if (lastChanged == null) {
                ancient++;
                buckets.merge("> 180 days", 1, Integer::sum);
                continue;
            }

            long ageDays = ChronoUnit.DAYS.between(lastChanged, now);
            totalAgeDays += ageDays;

            if (ageDays < AGING_PASSWORD_THRESHOLD_DAYS) {
                fresh++;
                buckets.merge("< 30 days", 1, Integer::sum);
            } else if (ageDays < OLD_PASSWORD_THRESHOLD_DAYS) {
                aging++;
                buckets.merge("30-90 days", 1, Integer::sum);
            } else if (ageDays < ANCIENT_PASSWORD_THRESHOLD_DAYS) {
                old++;
                buckets.merge("90-180 days", 1, Integer::sum);
            } else {
                ancient++;
                buckets.merge("> 180 days", 1, Integer::sum);
            }
        }

        List<PasswordAgeResponse.AgeDistributionBucket> distribution = List.of(
                PasswordAgeResponse.AgeDistributionBucket.builder()
                        .label("< 30 days").count(buckets.get("< 30 days")).minDays(0).maxDays(29).build(),
                PasswordAgeResponse.AgeDistributionBucket.builder()
                        .label("30-90 days").count(buckets.get("30-90 days")).minDays(30).maxDays(90).build(),
                PasswordAgeResponse.AgeDistributionBucket.builder()
                        .label("90-180 days").count(buckets.get("90-180 days")).minDays(91).maxDays(180).build(),
                PasswordAgeResponse.AgeDistributionBucket.builder()
                        .label("> 180 days").count(buckets.get("> 180 days")).minDays(181).maxDays(Integer.MAX_VALUE)
                        .build());

        int total = entries.size();
        return PasswordAgeResponse.builder()
                .totalPasswords(total)
                .freshCount(fresh)
                .agingCount(aging)
                .oldCount(old)
                .ancientCount(ancient)
                .averageAgeInDays(total == 0 ? 0 : totalAgeDays / total)
                .distribution(distribution)
                .build();
    }

    public List<VaultEntryResponse> getWeakPasswordsList(Long userId) {
        List<DecryptedVaultEntryDTO> entries = vaultAnalysisClient.getDecryptedEntries(userId);
        if (entries == null) entries = new ArrayList<>();
        List<VaultEntryResponse> weakEntries = new ArrayList<>();

        for (DecryptedVaultEntryDTO entry : entries) {
            String decrypted = entry.getDecryptedPassword();
            if (decrypted == null) decrypted = "";
            int score = passwordStrengthCalculator.calculateScore(decrypted);
            if (score < 40) {
                weakEntries.add(mapToResponse(entry, score));
            }
        }
        return weakEntries;
    }

    public List<VaultEntryResponse> getOldPasswordsList(Long userId) {
        List<DecryptedVaultEntryDTO> entries = vaultAnalysisClient.getDecryptedEntries(userId);
        if (entries == null) entries = new ArrayList<>();
        List<VaultEntryResponse> oldEntries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (DecryptedVaultEntryDTO entry : entries) {
            LocalDateTime lastChanged = entry.getUpdatedAt() != null ? entry.getUpdatedAt() : entry.getCreatedAt();
            if (lastChanged == null) {
                oldEntries.add(mapToResponse(entry, 0));
            } else {
                long ageDays = ChronoUnit.DAYS.between(lastChanged, now);
                if (ageDays > OLD_PASSWORD_THRESHOLD_DAYS) {
                    oldEntries.add(mapToResponse(entry, 0));
                }
            }
        }
        return oldEntries;
    }

    private VaultEntryResponse mapToResponse(DecryptedVaultEntryDTO e, int strengthScore) {
        String strengthLabel = strengthScore > 0 ? passwordStrengthCalculator.getStrengthLabel(strengthScore) : null;
        return VaultEntryResponse.builder()
                .id(e.getEntryId())
                .title(e.getTitle())
                .websiteUrl(e.getWebsiteUrl())
                .categoryId(e.getCategoryId())
                .categoryName(e.getCategoryName())
                .isFavorite(e.getIsFavorite())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .strengthScore(strengthScore)
                .strengthLabel(strengthLabel)
                .build();
    }

    int computeOverallScore(int total, int weakCount, int reusedCount, int oldCount) {
        if (total == 0) return 100;
        double score = 100.0;
        score -= ((double) weakCount / total) * 40;
        score -= ((double) reusedCount / total) * 30;
        score -= ((double) oldCount / total) * 20;
        return (int) Math.max(0, Math.min(100, score));
    }

    String getScoreLabel(int score) {
        if (score >= 90) return "Excellent";
        if (score >= 75) return "Good";
        if (score >= 50) return "Fair";
        if (score >= 25) return "Poor";
        return "Critical";
    }

    private String buildRecommendation(int weakCount, int reusedCount, int oldCount) {
        if (weakCount > 0 && weakCount >= reusedCount && weakCount >= oldCount) {
            return String.format("You have %d weak password(s). Strengthen them to improve your security score.", weakCount);
        }
        if (reusedCount > 0 && reusedCount >= oldCount) {
            return String.format("You are reusing passwords across %d accounts. Use unique passwords for each account.", reusedCount);
        }
        if (oldCount > 0) {
            return String.format("%d password(s) haven't been updated in over 90 days. Consider rotating them.", oldCount);
        }
        return "Your vault looks healthy! Keep maintaining strong, unique passwords.";
    }
}
