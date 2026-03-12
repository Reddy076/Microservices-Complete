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
public class BreachHistoryResponse {

    private List<ScanHistoryEntry> scanHistory;
    private int totalScans;
    private int totalCompromisedFound;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScanHistoryEntry {
        private Long scanId;
        private String triggerType;
        private int entriesScanned;
        private int compromisedFound;
        private String status;
        private LocalDateTime scannedAt;
    }
}
