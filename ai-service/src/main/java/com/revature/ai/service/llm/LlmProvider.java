package com.revature.ai.service.llm;

/**
 * Interface for LLM provider implementations
 */
public interface LlmProvider {
    
    /**
     * Generate a completion from the LLM
     * @param systemPrompt System prompt to guide the model
     * @param userPrompt User prompt/input
     * @return Generated text response
     */
    String generateCompletion(String systemPrompt, String userPrompt);
    
    /**
     * Generate text without enforcing JSON format
     * @param systemPrompt System prompt
     * @param userPrompt User prompt
     * @return Generated text response
     */
    String generateText(String systemPrompt, String userPrompt);
    
    /**
     * Check if the LLM provider is healthy/available
     * @return true if provider is reachable
     */
    boolean isHealthy();
    
    /**
     * Get the provider name
     * @return Provider name (OPENAI, ANTHROPIC, etc.)
     */
    String getProviderName();
}
