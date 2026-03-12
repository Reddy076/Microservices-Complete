package com.revature.security.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordAgeResponse {

    private int totalPasswords;
    private int freshCount;
    private int agingCount;
    private int oldCount;
    private int ancientCount;
    private double averageAgeInDays;
    private List<AgeDistributionBucket> distribution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgeDistributionBucket {
        private String label;
        private int count;
        private int minDays;
        private int maxDays;
    }
}

