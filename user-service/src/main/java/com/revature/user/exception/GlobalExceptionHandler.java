package com.revature.user.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
    logger.error("Resource not found: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
  }

  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<Object> handleRateLimitExceededException(RateLimitExceededException ex, WebRequest request) {
    logger.warn("Rate limit exceeded: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Object> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
    logger.error("Authentication error: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<Object> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
    logger.error("Bad credentials: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password", request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
    logger.error("Access denied: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.FORBIDDEN, "You do not have permission to access this resource", request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
    logger.error("Illegal argument: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });

    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("error", "Validation Failed");
    body.put("details", errors);

    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<Object> handleMethodNotSupported(
      org.springframework.web.HttpRequestMethodNotSupportedException ex, WebRequest request) {
    logger.error("Method not allowed: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request);
  }

  @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
  public ResponseEntity<Object> handleMissingParams(
      org.springframework.web.bind.MissingServletRequestParameterException ex, WebRequest request) {
    logger.error("Missing parameter: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.BAD_REQUEST, "Missing required parameter: " + ex.getParameterName(), request);
  }

  @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
  public ResponseEntity<Object> handleMessageNotReadable(
      org.springframework.http.converter.HttpMessageNotReadableException ex, WebRequest request) {
    logger.error("Request body not readable: {}", ex.getMessage(), ex);
    String message = "Invalid request body format";
    if (ex.getCause() != null) {
      message += ": " + ex.getCause().getMessage();
    }
    return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleGlobalException(Exception ex, WebRequest request) {
    logger.error("Unexpected error: ", ex);
    return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
        "Error: " + ex.getMessage() + (ex.getCause() != null ? " Cause: " + ex.getCause().getMessage() : ""), request);
  }

  private ResponseEntity<Object> buildErrorResponse(HttpStatus status, String message, WebRequest request) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("message", message);
    body.put("path", request.getDescription(false).replace("uri=", ""));

    return new ResponseEntity<>(body, status);
  }
}
