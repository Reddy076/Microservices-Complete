package com.revature.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for LLM cloud providers (OpenAI, Anthropic, etc.)
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LlmConfig {
    
    /**
     * LLM provider: OPENAI, ANTHROPIC, GEMINI
     */
    private String provider = "OPENAI";
    
    /**
     * Base URL for the LLM API
     * OpenAI: https://api.openai.com/v1
     * Anthropic: https://api.anthropic.com
     * Gemini: https://generativelanguage.googleapis.com/v1
     */
    private String baseUrl = "https://api.openai.com/v1";
    
    /**
     * API key for the LLM provider
     */
    private String apiKey;
    
    /**
     * Model name to use
     * OpenAI: gpt-4, gpt-4-turbo, gpt-3.5-turbo
     * Anthropic: claude-3-opus, claude-3-sonnet, claude-3-haiku
     */
    private String model = "gpt-4";
    
    /**
     * Temperature for generation (0.0 - 2.0)
     */
    private double temperature = 0.7;
    
    /**
     * Request timeout in seconds
     */
    private int timeoutSeconds = 60;
    
    /**
     * Maximum tokens to generate
     */
    private int maxTokens = 2048;
    
    /**
     * Whether to enable streaming responses
     */
    private boolean streamEnabled = false;
    
    /**
     * Maximum retries for failed requests
     */
    private int maxRetries = 3;
}
