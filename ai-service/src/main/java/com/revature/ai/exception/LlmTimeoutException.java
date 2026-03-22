package com.revature.ai.exception;

/**
 * Exception thrown when LLM request times out
 */
public class LlmTimeoutException extends RuntimeException {
    
    public LlmTimeoutException(String message) {
        super(message);
    }
    
    public LlmTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
