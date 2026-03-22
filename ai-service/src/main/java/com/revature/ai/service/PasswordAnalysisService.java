package com.revature.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.ai.config.LlmConfig;
import com.revature.ai.dto.response.PasswordAnalysisResponse;
import com.revature.ai.service.llm.LlmClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for analyzing password strength using LLM with a local rule-based fallback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordAnalysisService {

    private final LlmClientService llmClient;
    private final ObjectMapper objectMapper;
    private final LlmConfig llmConfig;

    private static final String SYSTEM_PROMPT = """
        You are a cybersecurity expert specializing in password security.
        Analyze the given password conceptually and provide:
        1. Strength rating (Must be exactly one of: VERY_WEAK, WEAK, MODERATE, STRONG, VERY_STRONG)
        2. List of specific vulnerabilities (array of strings)
        3. Actionable improvement suggestions (array of strings, max 3)
        
        Keep responses concise, factual, and user-friendly. Do not include markdown formatting or extra conversational text.
        Format your response strictly as valid JSON with keys: "strength", "vulnerabilities", "suggestions"
        """;

    /**
     * Analyze a password and return security analysis.
     * Uses LLM if configured; otherwise falls back to local rule-based analysis.
     *
     * @param password The password to analyze
     * @return PasswordAnalysisResponse with strength, vulnerabilities, and suggestions
     */
    public PasswordAnalysisResponse analyzePassword(String password) {
        // If no API key is configured, skip the LLM call entirely
        String apiKey = llmConfig.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("No LLM API key configured. Falling back to local rule-based password analysis.");
            return analyzePasswordLocally(password);
        }

        String userPrompt = "Analyze this password: '" + password + "'";
        try {
            log.debug("Sending password analysis request to LLM");
            String llmResponseText = llmClient.generateCompletion(SYSTEM_PROMPT, userPrompt);
            return objectMapper.readValue(llmResponseText, PasswordAnalysisResponse.class);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM JSON response for password analysis; using local fallback", e);
            return analyzePasswordLocally(password);
        } catch (Exception e) {
            log.error("LLM call failed during password analysis; using local fallback", e);
            return analyzePasswordLocally(password);
        }
    }

    // -----------------------------------------------------------------------
    // Local rule-based fallback (no external service required)
    // -----------------------------------------------------------------------

    private static final Pattern HAS_UPPER   = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWER   = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT   = Pattern.compile("\\d");
    private static final Pattern HAS_SPECIAL = Pattern.compile("[^A-Za-z0-9]");
    private static final List<String> COMMON_PASSWORDS = List.of(
            "password", "123456", "123456789", "qwerty", "abc123",
            "password1", "letmein", "iloveyou", "admin", "welcome"
    );

    /**
     * Deterministic, heuristic-based password analysis used when the LLM is unavailable.
     */
    private PasswordAnalysisResponse analyzePasswordLocally(String password) {
        List<String> vulnerabilities = new ArrayList<>();
        List<String> suggestions     = new ArrayList<>();

        boolean hasUpper   = HAS_UPPER.matcher(password).find();
        boolean hasLower   = HAS_LOWER.matcher(password).find();
        boolean hasDigit   = HAS_DIGIT.matcher(password).find();
        boolean hasSpecial = HAS_SPECIAL.matcher(password).find();
        int     length     = password.length();

        // Check common passwords
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            vulnerabilities.add("This is a very commonly used password and will be guessed immediately.");
            suggestions.add("Choose a unique passphrase that is not on any common-password list.");
        }

        // Length checks
        if (length < 8) {
            vulnerabilities.add("Password is too short (less than 8 characters).");
            suggestions.add("Use at least 12 characters for a strong password.");
        } else if (length < 12) {
            vulnerabilities.add("Password length is acceptable but could be longer.");
            suggestions.add("Aim for 16+ characters to significantly increase security.");
        }

        // Character variety
        if (!hasUpper) {
            vulnerabilities.add("No uppercase letters — reduces complexity.");
            suggestions.add("Add uppercase letters (A–Z).");
        }
        if (!hasLower) {
            vulnerabilities.add("No lowercase letters — reduces complexity.");
            suggestions.add("Add lowercase letters (a–z).");
        }
        if (!hasDigit) {
            vulnerabilities.add("No numbers — reduces complexity.");
            suggestions.add("Include at least one digit (0–9).");
        }
        if (!hasSpecial) {
            vulnerabilities.add("No special characters — reduces complexity.");
            suggestions.add("Add special characters such as !@#$%^&*.");
        }

        // Repetition check
        if (password.matches("(.)\\1{3,}")) {
            vulnerabilities.add("Password contains long runs of repeated characters.");
            suggestions.add("Avoid repeating the same character more than twice in a row.");
        }

        // Score → strength mapping
        int score = 0;
        if (length >= 8)   score++;
        if (length >= 12)  score++;
        if (length >= 16)  score++;
        if (hasUpper)      score++;
        if (hasLower)      score++;
        if (hasDigit)      score++;
        if (hasSpecial)    score++;
        if (!COMMON_PASSWORDS.contains(password.toLowerCase())) score++;

        String strength;
        if      (score <= 2) strength = "VERY_WEAK";
        else if (score <= 4) strength = "WEAK";
        else if (score <= 5) strength = "MODERATE";
        else if (score <= 6) strength = "STRONG";
        else                 strength = "VERY_STRONG";

        // Always provide at least one suggestion
        if (suggestions.isEmpty()) {
            suggestions.add("Great password! Consider using a password manager to keep it safe.");
        }

        log.debug("Local password analysis complete — strength: {}", strength);
        return PasswordAnalysisResponse.builder()
                .strength(strength)
                .vulnerabilities(vulnerabilities)
                .suggestions(suggestions.subList(0, Math.min(3, suggestions.size())))
                .build();
    }
}
