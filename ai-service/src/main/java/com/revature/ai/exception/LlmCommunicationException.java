package com.revature.ai.exception;

/**
 * Exception thrown when communication with LLM service fails
 */
public class LlmCommunicationException extends RuntimeException {
    
    public LlmCommunicationException(String message) {
        super(message);
    }
    
    public LlmCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
