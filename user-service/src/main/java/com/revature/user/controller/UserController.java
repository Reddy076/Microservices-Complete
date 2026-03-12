package com.revature.user.controller;

import com.revature.user.dto.request.AccountDeletionRequest;
import com.revature.user.service.user.AccountDeletionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.revature.user.dto.SecurityQuestionDTO;
import com.revature.user.dto.request.ChangePasswordRequest;
import com.revature.user.dto.request.UpdateSecurityQuestionsRequest;
import com.revature.user.dto.request.ToggleReadOnlyRequest;
import com.revature.user.dto.response.MessageResponse;
import com.revature.user.dto.response.UserResponse;
import com.revature.user.dto.response.UserSettingsResponse;
import com.revature.user.exception.AuthenticationException;
import com.revature.user.exception.ResourceNotFoundException;
import com.revature.user.model.user.SecurityQuestion;
import com.revature.user.model.user.User;
import com.revature.user.repository.UserRepository;
import com.revature.user.service.auth.SecurityQuestionService;
import com.revature.user.service.user.UserService;
import com.revature.user.service.user.UserSettingsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final AccountDeletionService accountDeletionService;
  private final UserService userService;
  private final UserRepository userRepository;
  private final SecurityQuestionService securityQuestionService;
  private final PasswordEncoder passwordEncoder;
  private final UserSettingsService userSettingsService;

  @GetMapping("/profile")
  public ResponseEntity<UserResponse> getProfile() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return ResponseEntity.ok(userService.getUserProfile(username));
  }

  @PutMapping("/profile")
  public ResponseEntity<UserResponse> updateProfile(
      @Valid @RequestBody com.revature.user.dto.request.UpdateProfileRequest request) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return ResponseEntity.ok(userService.updateProfile(username, request));
  }

  @PutMapping("/change-password")
  public ResponseEntity<MessageResponse> changePassword(
      @Valid @RequestBody ChangePasswordRequest request) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    userService.changeMasterPassword(username, request);
    return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
  }

  @DeleteMapping("/account")
  public ResponseEntity<MessageResponse> deleteAccount(@Valid @RequestBody AccountDeletionRequest request) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    accountDeletionService.scheduleAccountDeletion(username, request);
    return ResponseEntity.ok(
        new MessageResponse(
            "Account scheduled for deletion in 30 days. You can cancel this action by logging in and using the cancel endpoint."));
  }

  @PostMapping("/account/cancel-deletion")
  public ResponseEntity<MessageResponse> cancelDeletion() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    accountDeletionService.cancelAccountDeletion(username);
    return ResponseEntity.ok(new MessageResponse("Account deletion cancelled."));
  }

  @GetMapping("/security-questions")
  public ResponseEntity<List<SecurityQuestionDTO>> getSecurityQuestions() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    User user = userRepository.findByUsername(username)
        .orElseThrow(
            () -> new ResourceNotFoundException("User", "username", username));

    List<SecurityQuestion> questions = securityQuestionService
        .getSecurityQuestions(user);

    List<SecurityQuestionDTO> questionDTOs = questions.stream()
        .map(q -> new SecurityQuestionDTO(q.getQuestionText(), ""))

        .collect(Collectors.toList());

    return ResponseEntity.ok(questionDTOs);
  }

  @PutMapping("/security-questions")
  public ResponseEntity<Void> updateSecurityQuestions(
      @Valid @RequestBody UpdateSecurityQuestionsRequest request) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    User user = userRepository.findByUsername(username)
        .orElseThrow(
            () -> new ResourceNotFoundException("User", "username", username));

    if (!passwordEncoder.matches(request.getMasterPassword(), user.getMasterPasswordHash())) {
      throw new AuthenticationException("Invalid master password");
    }

    securityQuestionService.saveSecurityQuestions(user, request.getSecurityQuestions());
    return ResponseEntity.ok().build();
  }



  @PutMapping("/read-only-mode")
  public ResponseEntity<UserSettingsResponse> toggleReadOnlyMode(
      @Valid @RequestBody ToggleReadOnlyRequest request) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return ResponseEntity.ok(userSettingsService.toggleReadOnlyMode(username, request.getReadOnlyMode()));
  }
}
