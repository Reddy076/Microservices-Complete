package com.revature.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.ai.dto.request.ChatRequest;
import com.revature.ai.dto.response.ChatResponse;
import com.revature.ai.service.llm.LlmClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for security chatbot functionality
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityChatbotService {

    private final LlmClientService llmClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        You are a helpful security assistant for a password manager application.
        You help users with:
        - Password security best practices
        - Generating strong passwords
        - Understanding security concepts
        - Answering questions about online security
        
        Be concise, friendly, and informative. When users ask about passwords,
        you can suggest generating strong passwords. When appropriate,
        detect their intent and include relevant information.
        
        Available actions:
        - Generate password: If user asks for a password or says "generate password", include a generated password in your response
        - Analyze password: If user shares a password for analysis, explain the security implications
        
        Always prioritize user security and never encourage poor security practices.
        """;

    /**
     * Process a chat message and return AI response
     * @param request Chat request containing the message
     * @return ChatResponse with AI response, intent, and optional generated password
     */
    public ChatResponse chat(ChatRequest request) {
        try {
            log.debug("Processing chat message: {}", request.getMessage());
            String responseText = llmClient.generateText(SYSTEM_PROMPT, request.getMessage());
            
            // Detect intent from the message
            String intent = detectIntent(request.getMessage());
            
            // Check if password should be generated
            boolean shouldGeneratePassword = intent.equals("GENERATE_PASSWORD") || 
                request.getMessage().toLowerCase().contains("generate password");
            
            String generatedPassword = null;
            if (shouldGeneratePassword) {
                generatedPassword = generatePassword();
            }
            
            return ChatResponse.builder()
                    .message(responseText)
                    .intent(intent)
                    .generatedPassword(generatedPassword)
                    .hasPassword(generatedPassword != null)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error during chat processing", e);
            return ChatResponse.builder()
                    .message("Sorry, I encountered an error processing your request. Please try again.")
                    .intent("ERROR")
                    .hasPassword(false)
                    .build();
        }
    }
    
    /**
     * Detect user intent from message
     */
    private String detectIntent(String message) {
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("generate password") || lowerMessage.contains("create password") || lowerMessage.contains("new password")) {
            return "GENERATE_PASSWORD";
        }
        if (lowerMessage.contains("analyze") || lowerMessage.contains("check") || lowerMessage.contains("strength")) {
            return "SECURITY_ANALYSIS";
        }
        if (lowerMessage.contains("suggest") || lowerMessage.contains("recommend") || lowerMessage.contains("best practice")) {
            return "SUGGESTION";
        }
        
        return "GENERAL_QUESTION";
    }
    
    /**
     * Generate a strong password
     */
    private String generatePassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        java.security.SecureRandom random = new java.security.SecureRandom();
        
        for (int i = 0; i < 16; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }
}
