package com.revature.ai.service.llm;

import com.revature.ai.config.LlmConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Main LLM client service that routes to the appropriate provider
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmClientService {

    private final LlmConfig config;
    private final OpenAiProvider openAiProvider;
    
    // Future: Add AnthropicProvider, GeminiProvider, etc.

    /**
     * Send prompt to LLM and get completion (JSON format enforced)
     */
    public String generateCompletion(String systemPrompt, String userPrompt) {
        log.debug("Generating completion with provider: {}", config.getProvider());
        
        switch (config.getProvider().toUpperCase()) {
            case "OPENAI":
                return openAiProvider.generateCompletion(systemPrompt, userPrompt);
            // Future cases:
            // case "ANTHROPIC":
            //     return anthropicProvider.generateCompletion(systemPrompt, userPrompt);
            default:
                log.warn("Unknown provider: {}, defaulting to OpenAI", config.getProvider());
                return openAiProvider.generateCompletion(systemPrompt, userPrompt);
        }
    }

    /**
     * Send prompt to LLM and get text response (no JSON enforcement)
     */
    public String generateText(String systemPrompt, String userPrompt) {
        log.debug("Generating text with provider: {}", config.getProvider());
        
        switch (config.getProvider().toUpperCase()) {
            case "OPENAI":
                return openAiProvider.generateText(systemPrompt, userPrompt);
            default:
                log.warn("Unknown provider: {}, defaulting to OpenAI", config.getProvider());
                return openAiProvider.generateText(systemPrompt, userPrompt);
        }
    }

    /**
     * Check if the LLM provider is healthy
     */
    public boolean isHealthy() {
        switch (config.getProvider().toUpperCase()) {
            case "OPENAI":
                return openAiProvider.isHealthy();
            default:
                return openAiProvider.isHealthy();
        }
    }

    /**
     * Get current provider name
     */
    public String getProviderName() {
        return config.getProvider();
    }
}
