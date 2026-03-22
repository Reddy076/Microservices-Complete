package com.revature.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.ai.dto.response.CategorizationResponse;
import com.revature.ai.service.llm.LlmClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for smart vault entry categorization using LLM
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmartVaultCategorizationService {

    private final LlmClientService llmClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        You are a credential categorizer for a password manager.
        Based on the provided URL, username, and title, suggest:
        1. Category (one of: WORK, PERSONAL, SOCIAL, FINANCE, SHOPPING, DEVELOPMENT, EDUCATION, ENTERTAINMENT, OTHER)
        2. Tags (array of relevant tags, max 5)
        3. Confidence score (0.0 to 1.0)
        
        Return JSON with keys: category, tags[], confidence
        """;

    /**
     * Categorize a vault entry based on its details
     * @param url The website URL
     * @param username The username/email
     * @param title The entry title
     * @return CategorizationResponse with suggested category and tags
     */
    public CategorizationResponse categorizeEntry(String url, String username, String title) {
        String userPrompt = String.format("""
            URL: %s
            Username: %s
            Title: %s
            """, url != null ? url : "N/A", username != null ? username : "N/A", title);

        try {
            log.debug("Sending categorization request to LLM for entry: {}", title);
            String llmResponseText = llmClient.generateCompletion(SYSTEM_PROMPT, userPrompt);
            
            return objectMapper.readValue(llmResponseText, CategorizationResponse.class);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM JSON response for categorization", e);
            return CategorizationResponse.builder()
                    .category("OTHER")
                    .tags(java.util.List.of("uncategorized"))
                    .confidence(0.0)
                    .build();
        } catch (Exception e) {
            log.error("Error during categorization sequence", e);
            throw new RuntimeException("AI Categorization Failed", e);
        }
    }
}
