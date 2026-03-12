package com.revature.security.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityScoreResponse {

    private int overallScore;
    private String scoreLabel;
    private int totalPasswords;
    private int strongPasswords;
    private int fairPasswords;
    private int weakPasswords;
    private int reusedPasswords;
    private int oldPasswords;
    private String recommendation;
}

