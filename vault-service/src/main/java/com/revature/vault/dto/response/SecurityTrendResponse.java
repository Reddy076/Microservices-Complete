package com.revature.vault.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityTrendResponse {

    private List<TrendDataPoint> trendPoints;
    private int scoreChange;
    private String trendDirection;
    private String periodLabel;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDataPoint {
        private LocalDateTime recordedAt;
        private int overallScore;
        private int weakPasswordsCount;
        private int reusedPasswordsCount;
        private int oldPasswordsCount;
    }
}
