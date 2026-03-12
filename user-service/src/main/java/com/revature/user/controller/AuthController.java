package com.revature.user.controller;

import com.revature.user.dto.SecurityQuestionDTO;
import com.revature.user.dto.request.LoginRequest;
import com.revature.user.dto.request.RecoveryRequest;
import com.revature.user.dto.request.RefreshTokenRequest;
import com.revature.user.dto.request.RegistrationRequest;
import com.revature.user.dto.request.ForgotPasswordRequest;
import com.revature.user.dto.request.VerifySecurityQuestionsRequest;
import com.revature.user.dto.request.VerifyCaptchaRequest;
import com.revature.user.dto.request.VerifyMasterPasswordRequest;
import com.revature.user.dto.response.AuthResponse;
import com.revature.user.dto.response.MessageResponse;
import com.revature.user.dto.response.UserResponse;
import com.revature.user.dto.response.TokenValidationResponse;
import com.revature.user.dto.response.PasswordHintResponse;
import com.revature.user.dto.request.SetDuressPasswordRequest;
import com.revature.user.dto.request.SetPasswordHintRequest;
import com.revature.user.exception.ResourceNotFoundException;
import com.revature.user.model.user.User;
import com.revature.user.repository.UserRepository;
import com.revature.user.service.auth.AccountRecoveryService;
import com.revature.user.service.auth.AuthenticationService;
import com.revature.user.service.auth.RegistrationService;
import com.revature.user.service.security.CaptchaService;
import com.revature.user.service.security.DuressService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for login, registration, and session management")
public class AuthController {

  private final RegistrationService registrationService;
  private final AuthenticationService authenticationService;
  private final AccountRecoveryService accountRecoveryService;
  private final DuressService duressService;
  private final CaptchaService captchaService;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Operation(summary = "Register a new user account")
  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(@Valid @RequestBody RegistrationRequest request) {
    UserResponse response = registrationService.registerUser(request);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  @Operation(summary = "Verify email with OTP code sent during registration")
  @PostMapping(value = "/verify-email", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<MessageResponse> verifyEmail(
      @RequestParam("username") String username,
      @RequestParam("code") String code) {
    registrationService.verifyEmail(username, code);
    return ResponseEntity.ok(new MessageResponse("Email verified successfully. You can now log in."));
  }

  @Operation(summary = "Resend email verification OTP code")
  @PostMapping(value = "/resend-verification-otp", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<MessageResponse> resendVerificationOtp(@RequestParam("username") String username) {
    registrationService.resendVerificationOtp(username);
    return ResponseEntity.ok(new MessageResponse("Verification code resent to your email."));
  }

  @Operation(summary = "Authenticate user and return JWT token")
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest) {

    if (captchaService.isCaptchaRequired(request.getUsername())) {
      if (request.getCaptchaToken() == null || request.getCaptchaToken().isBlank()) {
        return ResponseEntity.badRequest().body(
            AuthResponse.builder().message("CAPTCHA verification required").build());
      }
      if (!captchaService.verifyCaptcha(request.getCaptchaToken())) {
        return ResponseEntity.badRequest().body(
            AuthResponse.builder().message("CAPTCHA verification failed").build());
      }
    }

    AuthResponse response = authenticationService.login(request, httpRequest);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Refresh an expired JWT session token")
  @PostMapping("/refresh-token")
  public ResponseEntity<AuthResponse> refreshToken(
      @Valid @RequestBody RefreshTokenRequest request,
      HttpServletRequest httpRequest) {
    AuthResponse response = authenticationService.refreshToken(request, httpRequest);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Logout user and invalidate token")
  @PostMapping("/logout")
  public ResponseEntity<MessageResponse> logout(@RequestHeader("Authorization") String authHeader) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    authenticationService.logout(authHeader, username);
    return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
  }

  @Operation(summary = "Get security questions for a given username")
  @GetMapping("/security-questions/{username}")
  public ResponseEntity<List<SecurityQuestionDTO>> getSecurityQuestions(
      @PathVariable String username) {
    return ResponseEntity.ok(authenticationService.getSecurityQuestions(username));
  }

  @Operation(summary = "Reset the master password using security questions")
  @PostMapping("/reset-password")
  public ResponseEntity<MessageResponse> resetPassword(
      @Valid @RequestBody RecoveryRequest request) {
    accountRecoveryService.resetPassword(request);
    return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
  }

  @Operation(summary = "Verify OTP code during 2FA login sequence")
  @PostMapping("/verify-otp")
  public ResponseEntity<AuthResponse> verifyOtp(
      @RequestParam String username,
      @RequestParam String code,
      HttpServletRequest httpRequest) {
    return ResponseEntity.ok(authenticationService.verifyOtp(username, code, httpRequest));
  }

  @Operation(summary = "Send OTP code to user's email")
  @PostMapping("/send-otp")
  public ResponseEntity<MessageResponse> sendOtp(@RequestParam String username) {
    authenticationService.sendOtp(username);
    return ResponseEntity.ok(new MessageResponse("OTP sent to your email."));
  }

  @Operation(summary = "Resend OTP code to user's email")
  @PostMapping("/resend-otp")
  public ResponseEntity<MessageResponse> resendOtp(@RequestParam String username) {
    authenticationService.sendOtp(username);
    return ResponseEntity.ok(new MessageResponse("OTP resent to your email."));
  }

  @Operation(summary = "Initiate account recovery via email link")
  @PostMapping("/forgot-password")
  public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    accountRecoveryService.forgotPassword(request);
    return ResponseEntity.ok(new MessageResponse("If the account exists, password recovery has been initiated."));
  }

  @Operation(summary = "Validate security question answers during recovery")
  @PostMapping("/verify-security-questions")
  public ResponseEntity<MessageResponse> verifySecurityQuestions(
      @Valid @RequestBody VerifySecurityQuestionsRequest request) {
    accountRecoveryService.verifySecurityQuestions(request);
    return ResponseEntity.ok(new MessageResponse("Security questions verified successfully"));
  }

  @Operation(summary = "Verify captcha token")
  @PostMapping("/verify-captcha")
  public ResponseEntity<MessageResponse> verifyCaptcha(@Valid @RequestBody VerifyCaptchaRequest request) {
    if (!captchaService.verifyCaptcha(request.getCaptchaToken())) {
      return ResponseEntity.badRequest().body(new MessageResponse("CAPTCHA verification failed"));
    }
    return ResponseEntity.ok(new MessageResponse("CAPTCHA verified successfully"));
  }

  @Operation(summary = "Check if current JWT token is active and valid")
  @GetMapping("/validate-token")
  public ResponseEntity<TokenValidationResponse> validateToken(@RequestHeader("Authorization") String authHeader) {
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);
      boolean isValid = authenticationService.validateToken(token);
      return ResponseEntity.ok(new TokenValidationResponse(isValid));
    }
    return ResponseEntity.badRequest().body(new TokenValidationResponse(false));
  }

  @Operation(summary = "Re-authenticate master password for sensitive actions")
  @PostMapping("/verify-master-password")
  public ResponseEntity<MessageResponse> verifyMasterPassword(@Valid @RequestBody VerifyMasterPasswordRequest request) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    authenticationService.verifyMasterPassword(username, request);
    return ResponseEntity.ok(new MessageResponse("Master password verified successfully"));
  }

  @Operation(summary = "Login using a duress password (wipes local vault data)")
  @PostMapping("/duress-login")
  public ResponseEntity<AuthResponse> duressLogin(@Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest) {
    AuthResponse response = authenticationService.duressLogin(request, httpRequest);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Configure a duress password for emergency data wiping")
  @PostMapping("/set-duress-password")
  public ResponseEntity<MessageResponse> setDuressPassword(@Valid @RequestBody SetDuressPasswordRequest request) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    duressService.setDuressPassword(username, request.getDuressPassword());
    return ResponseEntity.ok(new MessageResponse("Duress password set successfully"));
  }

  @Operation(summary = "Retrieve password hint for given username")
  @GetMapping("/password-hint/{username}")
  public ResponseEntity<PasswordHintResponse> getPasswordHint(@PathVariable String username) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    String hint = user.getPasswordHint() != null ? user.getPasswordHint() : "No hint set";
    return ResponseEntity.ok(new PasswordHintResponse(hint));
  }

  @Operation(summary = "Set or update the password hint")
  @PutMapping(value = "/password-hint", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<MessageResponse> setPasswordHint(@Valid @RequestBody SetPasswordHintRequest request) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    String newHint = request.getHint();
    String masterPassword = request.getMasterPassword();

    if (!passwordEncoder.matches(masterPassword, user.getMasterPasswordHash())) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Invalid master password"));
    }

    if (newHint != null && !newHint.trim().isEmpty()) {
      if (newHint.toLowerCase().contains(masterPassword.toLowerCase())) {
        return ResponseEntity.badRequest()
            .body(new MessageResponse("Password hint cannot contain the master password"));
      }
    }

    user.setPasswordHint(newHint);
    userRepository.save(user);
    return ResponseEntity.ok(new MessageResponse("Password hint updated"));
  }
}
