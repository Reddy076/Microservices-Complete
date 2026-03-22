package com.revature.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for vault entry categorization
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorizationResponse {
    
    /**
     * Suggested category
     * WORK, PERSONAL, SOCIAL, FINANCE, SHOPPING, DEVELOPMENT, EDUCATION, ENTERTAINMENT, OTHER
     */
    private String category;
    
    /**
     * Suggested tags (max 5)
     */
    private List<String> tags;
    
    /**
     * Confidence score (0.0 to 1.0)
     */
    private double confidence;
}
