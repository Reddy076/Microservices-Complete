package com.revature.user.controller;

import com.revature.user.dto.response.TwoFactorSetupResponse;
import com.revature.user.model.user.User;
import com.revature.user.repository.UserRepository;
import com.revature.user.service.auth.SessionService;
import com.revature.user.service.auth.TwoFactorService;
import com.revature.user.exception.AuthenticationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.revature.user.dto.response.TwoFactorStatusResponse;
import com.revature.user.dto.response.VerifySetupResponse;
import com.revature.user.dto.response.RegenerateCodesResponse;
import com.revature.user.dto.response.MessageResponse;

@RestController
@RequestMapping("/api/2fa")
@RequiredArgsConstructor
@Tag(name = "Two-Factor Auth Controller", description = "Endpoints for managing 2FA")
public class TwoFactorController {

  private final TwoFactorService twoFactorService;
  private final SessionService sessionService;
  private final UserRepository userRepository;

  @GetMapping("/status")
  @Operation(summary = "Get 2FA status")
  public ResponseEntity<TwoFactorStatusResponse> getStatus(HttpServletRequest request) {
    User user = getUserBySession(request);
    return ResponseEntity.ok(new TwoFactorStatusResponse(user.is2faEnabled()));
  }

  @PostMapping("/setup")
  @Operation(summary = "Initialize 2FA setup (get QR code)")
  public ResponseEntity<TwoFactorSetupResponse> setup2FA(HttpServletRequest request) {
    User user = getUserBySession(request);
    TwoFactorSetupResponse response = twoFactorService.setup2FA(user);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/verify-setup")
  @Operation(summary = "Verify and enable 2FA")
  public ResponseEntity<VerifySetupResponse> verifySetup(
      @RequestParam String code,
      HttpServletRequest request) {
    User user = getUserBySession(request);
    twoFactorService.verifySetup(user, code);
    return ResponseEntity.ok(new VerifySetupResponse(
        true,
        "2FA enabled successfully",
        twoFactorService.getRecoveryCodes(user)));
  }

  @PostMapping("/disable")
  @Operation(summary = "Disable 2FA")
  public ResponseEntity<MessageResponse> disable2FA(HttpServletRequest request) {
    User user = getUserBySession(request);
    twoFactorService.disable2FA(user);
    return ResponseEntity.ok(new MessageResponse("2FA disabled successfully"));
  }

  @GetMapping("/backup-codes")
  @Operation(summary = "Get backup recovery codes")
  public ResponseEntity<List<String>> getBackupCodes(HttpServletRequest request) {
    User user = getUserBySession(request);
    return ResponseEntity.ok(twoFactorService.getRecoveryCodes(user));
  }

  @PostMapping("/regenerate-codes")
  @Operation(summary = "Regenerate backup recovery codes")
  public ResponseEntity<RegenerateCodesResponse> regenerateCodes(HttpServletRequest request) {
    User user = getUserBySession(request);
    List<String> newCodes = twoFactorService.regenerateRecoveryCodes(user);
    return ResponseEntity.ok(new RegenerateCodesResponse(
        true,
        "Backup codes regenerated successfully",
        newCodes));
  }

  private User getUserBySession(HttpServletRequest request) {

    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new AuthenticationException("Missing or invalid Authorization header");
    }
    String token = authHeader.substring(7);

    Authentication authentication = SecurityContextHolder
        .getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AuthenticationException("User not authenticated");
    }

    Object principal = authentication.getPrincipal();
    String username;
    if (principal instanceof UserDetails) {
      username = ((UserDetails) principal).getUsername();
    } else {
      username = principal.toString();
    }

    String finalUsername = username;
    return userRepository.findByUsername(username)
        .or(() -> userRepository.findByEmail(finalUsername))
        .orElseThrow(() -> new AuthenticationException("User not found"));
  }
}
