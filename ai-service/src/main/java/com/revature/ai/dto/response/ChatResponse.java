package com.revature.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for chatbot messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    
    /**
     * AI response message
     */
    private String message;
    
    /**
     * Detected intent (GENERATE_PASSWORD, SECURITY_ANALYSIS, etc.)
     */
    private String intent;
    
    /**
     * Generated password if applicable
     */
    private String generatedPassword;
    
    /**
     * Whether the response includes a generated password
     */
    private boolean hasPassword;
}
