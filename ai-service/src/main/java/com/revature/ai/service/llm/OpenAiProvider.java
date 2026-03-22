package com.revature.ai.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.revature.ai.config.LlmConfig;
import com.revature.ai.exception.LlmCommunicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI LLM provider implementation
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiProvider implements LlmProvider {

    private final LlmConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Override
    public String generateCompletion(String systemPrompt, String userPrompt) {
        String url = config.getBaseUrl() + "/chat/completions";

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", config.getModel());
        payload.put("temperature", config.getTemperature());
        payload.put("max_tokens", config.getMaxTokens());
        // response_format must be a JSON object {"type": "json_object"}, not a plain string
        ObjectNode responseFormat = objectMapper.createObjectNode();
        responseFormat.put("type", "json_object");
        payload.set("response_format", responseFormat);

        ArrayNode messages = payload.putArray("messages");
        
        // System message
        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        // User message
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);

        return executeRequest(url, payload);
    }

    @Override
    public String generateText(String systemPrompt, String userPrompt) {
        String url = config.getBaseUrl() + "/chat/completions";

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", config.getModel());
        payload.put("temperature", config.getTemperature());
        payload.put("max_tokens", config.getMaxTokens());

        ArrayNode messages = payload.putArray("messages");
        
        // System message
        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        // User message
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);

        return executeRequest(url, payload);
    }

    private String executeRequest(String url, ObjectNode payload) {
        try {
            RequestBody body = RequestBody.create(
                    objectMapper.writeValueAsString(payload),
                    JSON
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    log.error("OpenAI API failed with status: {} - {}", response.code(), errorBody);
                    throw new LlmCommunicationException("Failed to generate completion: " + response.code() + " - " + errorBody);
                }

                if (response.body() == null) {
                    throw new LlmCommunicationException("Empty response body from OpenAI");
                }

                String responseBodyStr = response.body().string();
                JsonNode responseNode = objectMapper.readTree(responseBodyStr);
                
                // Extract content from the response
                JsonNode choices = responseNode.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    JsonNode message = choices.get(0).path("message");
                    String content = message.path("content").asText();
                    return content;
                }

                throw new LlmCommunicationException("Invalid response format from OpenAI");
            }
        } catch (IOException e) {
            log.error("Error communicating with OpenAI API", e);
            throw new LlmCommunicationException("Error communicating with LLM service", e);
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            Request request = new Request.Builder()
                    .url(config.getBaseUrl() + "/models")
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            } catch (IOException e) {
                log.debug("OpenAI is not healthy or unreachable", e);
                return false;
            }
        } catch (Exception e) {
            log.debug("OpenAI health check failed", e);
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "OPENAI";
    }
}
