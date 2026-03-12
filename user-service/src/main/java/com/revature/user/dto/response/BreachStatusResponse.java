package com.revature.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreachStatusResponse {

    /** SAFE, AT_RISK, or COMPROMISED */
    private String overallStatus;
    private int totalCompromised;
    private int totalVaultEntries;
    private LocalDateTime lastScanAt;
    private boolean scanInProgress;
    private String recommendation;
}
