package com.revature.user.dto.response;

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
public class BreachScanResponse {

    private Long scanId;
    private int entriesScanned;
    private int compromisedFound;
    private String status;
    private String triggerType;
    private LocalDateTime scannedAt;
    private List<CompromisedCredentialResponse> newlyCompromised;
}
