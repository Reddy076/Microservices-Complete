package com.revature.user.service.auth;

import com.revature.user.dto.request.RegistrationRequest;
import com.revature.user.dto.response.UserResponse;
import com.revature.user.exception.AuthenticationException;
import com.revature.user.model.user.User;
import com.revature.user.repository.UserRepository;
import com.revature.user.service.email.EmailService;
import com.revature.user.client.VaultClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final SecurityQuestionService securityQuestionService;
  private final OtpService otpService;
  private final EmailService emailService;
  private final VaultClient vaultClient;

  @Transactional
  public UserResponse registerUser(RegistrationRequest request) {

    if (userRepository.existsByEmail(request.getEmail())) {
      throw new AuthenticationException("Email already in use");
    }
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new AuthenticationException("Username already in use");
    }

    if (request.getPasswordHint() != null && !request.getPasswordHint().trim().isEmpty() && request.getPasswordHint().toLowerCase().contains(request.getMasterPassword().toLowerCase())) {
        throw new AuthenticationException("Password hint cannot contain the master password");
    }

    User user = User.builder()
        .email(request.getEmail())
        .username(request.getUsername())
        .masterPasswordHash(passwordEncoder.encode(request.getMasterPassword()))
        .passwordHint(request.getPasswordHint())
        .salt(UUID.randomUUID().toString())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .is2faEnabled(false)
        .emailVerified(false)
        .build();

    User savedUser = userRepository.save(user);

    // Call VaultClient to create defaults
    try {
        vaultClient.createDefaultFoldersAndCategories(savedUser.getId());
    } catch (Exception e) {
        // Log and continue, as this is non-critical for registration success
    }

    securityQuestionService.saveSecurityQuestions(savedUser, request.getSecurityQuestions());

    String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
    String otpCode = otpService.generateOtp(savedUser, EMAIL_VERIFICATION);
    emailService.sendOtpEmail(savedUser.getEmail(), otpCode);

    return UserResponse.builder()
        .id(savedUser.getId())
        .email(savedUser.getEmail())
        .username(savedUser.getUsername())
        .is2faEnabled(savedUser.is2faEnabled())
        .createdAt(savedUser.getCreatedAt())
        .build();
  }

  @Transactional
  public void verifyEmail(String username, String otpCode) {
    User user = userRepository.findByUsername(username)
        .or(() -> userRepository.findByEmail(username))
        .orElseThrow(() -> new AuthenticationException("User not found"));

    if (Boolean.TRUE.equals(user.getEmailVerified())) {
      throw new AuthenticationException("Email is already verified");
    }

    String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
    otpService.validateOtp(user, otpCode, EMAIL_VERIFICATION);

    user.setEmailVerified(true);
    userRepository.save(user);
  }

  @Transactional
  public void resendVerificationOtp(String username) {
    User user = userRepository.findByUsername(username)
        .or(() -> userRepository.findByEmail(username))
        .orElseThrow(() -> new AuthenticationException("User not found"));

    if (Boolean.TRUE.equals(user.getEmailVerified())) {
      throw new AuthenticationException("Email is already verified");
    }

    String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
    String otpCode = otpService.generateOtp(user, EMAIL_VERIFICATION);
    emailService.sendOtpEmail(user.getEmail(), otpCode);
  }
}
