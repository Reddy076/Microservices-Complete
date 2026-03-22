package com.revature.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for password analysis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordAnalysisResponse {
    
    /**
     * Password strength rating
     * VERY_WEAK, WEAK, MODERATE, STRONG, VERY_STRONG
     */
    private String strength;
    
    /**
     * List of specific vulnerabilities
     */
    private List<String> vulnerabilities;
    
    /**
     * Actionable improvement suggestions
     */
    private List<String> suggestions;
}
