package com.revature.vault.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordHealthMetricsResponse {

    private int totalPasswords;
    private int strongCount;
    private int goodCount;
    private int fairCount;
    private int weakCount;
    private int veryWeakCount;
    private double averageStrengthScore;
    private List<PasswordCategoryBreakdown> categoryBreakdowns;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordCategoryBreakdown {
        private String categoryName;
        private int count;
        private double averageScore;
        private int weakCount;
    }
}
