package com.revature.ai.controller;

import com.revature.ai.dto.request.ChatRequest;
import com.revature.ai.dto.response.CategorizationResponse;
import com.revature.ai.dto.response.ChatResponse;
import com.revature.ai.dto.response.PasswordAnalysisResponse;
import com.revature.ai.service.PasswordAnalysisService;
import com.revature.ai.service.SecurityChatbotService;
import com.revature.ai.service.SmartVaultCategorizationService;
import com.revature.ai.service.llm.LlmClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for AI features
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final LlmClientService llmClientService;
    private final PasswordAnalysisService passwordAnalysisService;
    private final SmartVaultCategorizationService categorizationService;
    private final SecurityChatbotService chatbotService;

    /**
     * Health check for AI service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        boolean healthy = llmClientService.isHealthy();
        
        if (healthy) {
            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "provider", llmClientService.getProviderName(),
                    "message", "AI service is operational"
            ));
        } else {
            return ResponseEntity.status(503).body(Map.of(
                    "status", "DOWN",
                    "message", "Failed to connect to LLM provider"
            ));
        }
    }

    /**
     * Analyze a password for security
     */
    @PostMapping("/analyze-password")
    public ResponseEntity<PasswordAnalysisResponse> analyzePassword(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        
        PasswordAnalysisResponse result = passwordAnalysisService.analyzePassword(password);
        return ResponseEntity.ok(result);
    }

    /**
     * Categorize a vault entry
     */
    @PostMapping("/categorize-entry")
    public ResponseEntity<CategorizationResponse> categorizeEntry(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        String username = request.get("username");
        String title = request.get("title");

        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        CategorizationResponse result = categorizationService.categorizeEntry(url, username, title);
        return ResponseEntity.ok(result);
    }

    /**
     * Security chatbot endpoint
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        ChatResponse response = chatbotService.chat(request);
        return ResponseEntity.ok(response);
    }
}
