package com.revature.user.service.auth;

import com.revature.user.config.JwtConfig;
import com.revature.user.dto.SecurityQuestionDTO;
import com.revature.user.dto.request.LoginRequest;
import com.revature.user.dto.request.RefreshTokenRequest;
import com.revature.user.dto.request.VerifyMasterPasswordRequest;
import com.revature.user.dto.response.AuthResponse;
import com.revature.user.exception.AuthenticationException;
import com.revature.user.model.user.User;
import com.revature.user.repository.UserRepository;
import com.revature.user.security.JwtTokenProvider;
import com.revature.user.service.email.EmailService;
import com.revature.user.client.SecurityClient;
import com.revature.user.service.security.LoginAttemptService;
import com.revature.user.service.security.AdaptiveAuthService;
import com.revature.user.service.security.DuressService;
import com.revature.user.service.security.GeoLocationService;
import com.revature.user.util.DeviceFingerprintUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;



@Service
@RequiredArgsConstructor
public class AuthenticationService {

  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtConfig jwtConfig;
  private final UserRepository userRepository;
  private final SessionService sessionService;
  private final SecurityQuestionService securityQuestionService;
  private final TwoFactorService twoFactorService;

  private final EmailService emailService;
  private final OtpService otpService;
  private final SecurityClient securityClient;
  private final AdaptiveAuthService adaptiveAuthService;
  private final GeoLocationService geoLocationService;
  private final DeviceFingerprintUtil deviceFingerprintUtil;
  private final DuressService duressService;
  private final LoginAttemptService loginAttemptService;

  public boolean validateToken(String token) {
    return jwtTokenProvider.validateToken(token);
  }

  @Transactional
  public void verifyMasterPassword(String username, VerifyMasterPasswordRequest request) {
    User user = userRepository.findByUsernameOrThrow(username);

    try {
      authenticationManager
          .authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), request.getMasterPassword()));
    } catch (org.springframework.security.core.AuthenticationException e) {
      throw new AuthenticationException("Invalid master password");
    }
  }

  @Transactional
  public void sendOtp(String username) {
    User user = userRepository.findByUsernameOrThrow(username);

    String otpCode = otpService.generateOtp(user, "EMAIL");

    emailService.sendOtpEmail(user.getEmail(), otpCode);
  }

  @Transactional
  public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
    String ip = httpRequest != null ? httpRequest.getRemoteAddr() : null;
    String deviceInfo = httpRequest != null ? httpRequest.getHeader("User-Agent") : null;
    String deviceFingerprint = deviceFingerprintUtil.generateFingerprint(httpRequest);
    String location = geoLocationService.getLocationFromIp(ip);

    User userForLockCheck = userRepository.findByUsername(request.getUsername())
        .or(() -> userRepository.findByEmail(request.getUsername()))
        .orElse(null);
    if (userForLockCheck != null && userForLockCheck.getLockedUntil() != null
        && userForLockCheck.getLockedUntil().isAfter(LocalDateTime.now())) {
      loginAttemptService.recordLoginAttempt(request.getUsername(), false, "Account locked", ip, deviceInfo, location,
          0);
      throw new AuthenticationException("Account is locked until " + userForLockCheck.getLockedUntil());
    }

    if (userForLockCheck != null && !Boolean.TRUE.equals(userForLockCheck.getEmailVerified())) {
      throw new AuthenticationException("Email not verified. Please check your email for the verification code.");
    }

    int riskScore = adaptiveAuthService.calculateRiskScore(request.getUsername(), ip, deviceInfo, deviceFingerprint);

    try {
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              request.getUsername(),
              request.getMasterPassword()));

      User user = userRepository.findByUsername(request.getUsername())
          .or(() -> userRepository.findByEmail(request.getUsername()))
          .orElseThrow(() -> new AuthenticationException("User not found"));

      if (user.getFailedLoginAttempts() > 0) {
        userRepository.resetFailedLoginAttempts(user.getUsername());
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
      }

      if (user.is2faEnabled() || adaptiveAuthService.requiresAdditionalVerification(riskScore)) {

        if (!user.is2faEnabled()) {
          sendOtp(user.getUsername());
        }
        return AuthResponse.builder()
            .requires2FA(true)
            .username(user.getUsername())
            .build();
      }

      String accessToken = jwtTokenProvider.generateAccessToken(authentication);
      String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

      sessionService.createSession(user, accessToken, httpRequest, location, deviceFingerprint);

      try {
        if (loginAttemptService.isNewDevice(user.getUsername(), deviceInfo)) {
          securityClient.createAlert(user.getUsername(),
              "NEW_DEVICE_LOGIN",
              "New Device Detected",
              "Login detected from a new device or browser: " + deviceInfo,
              "MEDIUM");
        }

        securityClient.logAudit(user.getUsername(),
            "LOGIN",
            "Successful login", ip);
        loginAttemptService.recordLoginAttempt(user.getUsername(), true, null, ip, deviceInfo, location, riskScore);
      } catch (Exception auditEx) {
        // security-service audit/alert unavailable — log and continue
        org.slf4j.LoggerFactory.getLogger(AuthenticationService.class)
            .warn("Audit/alert call failed (security-service may be starting): {}", auditEx.getMessage());
      }

      return AuthResponse.builder()
          .accessToken(accessToken)
          .refreshToken(refreshToken)
          .username(user.getUsername())
          .expiresIn(jwtConfig.getAccessTokenExpiration())
          .requires2FA(false)
          .build();

    } catch (org.springframework.security.core.AuthenticationException e) {

      if (userForLockCheck != null) {
        userRepository.incrementFailedLoginAttempts(userForLockCheck.getUsername());

        User updatedUser = userRepository.findById(userForLockCheck.getId()).orElse(userForLockCheck);
        int attempts = updatedUser.getFailedLoginAttempts();

        if (attempts >= 3) {
          securityClient.createAlert(request.getUsername(),
              "MULTIPLE_FAILED_LOGINS",
              "Multiple Failed Login Attempts",
              attempts + " failed login attempts detected from IP: "
                  + (httpRequest != null ? httpRequest.getRemoteAddr() : "unknown"),
              "HIGH");
        }

        if (attempts >= 5) {
          int lockoutCount = updatedUser.getLockoutCount() + 1;
          long lockMinutes = (long) Math.min(15.0 * Math.pow(2.0, lockoutCount - 1), 1440.0);
          updatedUser.setLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
          updatedUser.setLockoutCount(lockoutCount);
          updatedUser.setFailedLoginAttempts(0);

          securityClient.createAlert(request.getUsername(),
              "ACCOUNT_LOCKED",
              "Account Locked",
              "Account locked for " + lockMinutes + " minutes due to " + attempts + " failed login attempts",
              "CRITICAL");

          userRepository.save(updatedUser);
        }
      }

      try {
        securityClient.logAudit(request.getUsername(),
            "LOGIN_FAILED",
            "Failed login attempt", ip);
        loginAttemptService.recordLoginAttempt(request.getUsername(), false, "Invalid credentials", ip, deviceInfo,
            location, riskScore);
      } catch (Exception auditEx) {
        org.slf4j.LoggerFactory.getLogger(AuthenticationService.class)
            .warn("Audit call failed on login failure: {}", auditEx.getMessage());
      }
      throw new AuthenticationException("Invalid username or password");
    }
  }

  @Transactional
  public AuthResponse refreshToken(RefreshTokenRequest request,
      HttpServletRequest httpRequest) {
    String refreshToken = request.getRefreshToken();

    if (!jwtTokenProvider.validateToken(refreshToken)) {
      throw new AuthenticationException("Invalid or expired refresh token");
    }

    String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
    User user = userRepository.findByUsernameOrThrow(username);

    UserDetails userDetails = new org.springframework.security.core.userdetails.User(
        user.getUsername(), user.getMasterPasswordHash(), Collections.emptyList());

    Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
        userDetails.getAuthorities());

    String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);
    String newRefreshToken = jwtTokenProvider.generateRefreshToken(authentication);

    String ip = httpRequest != null ? httpRequest.getRemoteAddr() : null;
    String location = geoLocationService.getLocationFromIp(ip);
    String deviceFingerprint = deviceFingerprintUtil.generateFingerprint(httpRequest);
    sessionService.createSession(user, newAccessToken, httpRequest, location, deviceFingerprint);

    return AuthResponse.builder()
        .accessToken(newAccessToken)
        .refreshToken(newRefreshToken)
        .username(user.getUsername())
        .expiresIn(jwtConfig.getAccessTokenExpiration())
        .requires2FA(user.is2faEnabled())
        .build();
  }

  @Transactional
  public AuthResponse duressLogin(LoginRequest request, HttpServletRequest httpRequest) {
    if (!duressService.isDuressLogin(request.getUsername(), request.getMasterPassword())) {
      throw new AuthenticationException("Invalid username or password");
    }

    User user = userRepository.findByUsername(request.getUsername())
        .or(() -> userRepository.findByEmail(request.getUsername()))
        .orElseThrow(() -> new AuthenticationException("User not found"));

    UserDetails userDetails = new org.springframework.security.core.userdetails.User(
        user.getUsername(), user.getMasterPasswordHash(), Collections.emptyList());

    Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
        userDetails.getAuthorities());

    String accessToken = jwtTokenProvider.generateDuressToken(authentication);
    String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

    String ip = httpRequest != null ? httpRequest.getRemoteAddr() : null;
    String location = geoLocationService.getLocationFromIp(ip);
    String deviceFingerprint = deviceFingerprintUtil.generateFingerprint(httpRequest);

    sessionService.createSession(user, accessToken, httpRequest, location, deviceFingerprint);

    return AuthResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .username(user.getUsername())
        .expiresIn(jwtConfig.getAccessTokenExpiration())
        .requires2FA(false)
        .build();
  }

  public void logout(String authHeader, String username) {
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String accessToken = authHeader.substring(7);

      sessionService.terminateSessionByToken(accessToken);
    }

    if (username != null) {
      securityClient.logAudit(username,
          "LOGOUT",
          "User logged out", "");
    }
  }

  @Transactional(readOnly = true)
  public List<SecurityQuestionDTO> getSecurityQuestions(String username) {
    User user = userRepository.findByUsernameOrThrow(username);

    return securityQuestionService
        .getSecurityQuestions(user).stream()
        .map(sq -> new SecurityQuestionDTO(sq.getQuestionText(), ""))
        .collect(Collectors.toList());
  }

  @Transactional
  public AuthResponse verifyOtp(String username, String code, HttpServletRequest httpRequest) {
    User user = userRepository.findByUsernameOrThrow(username);

    if (user.is2faEnabled()) {
      if (!twoFactorService.verifyCode(user, code)) {
        throw new AuthenticationException("Invalid 2FA code");
      }
    } else {
      try {
        otpService.validateOtp(user, code, "EMAIL");
      } catch (AuthenticationException e) {
        throw new AuthenticationException("Invalid OTP code");
      }
    }

    try {
      UserDetails userDetails = new org.springframework.security.core.userdetails.User(
          user.getUsername(), user.getMasterPasswordHash(), Collections.emptyList());

      Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
          userDetails.getAuthorities());

      String accessToken = jwtTokenProvider.generateAccessToken(authentication);
      String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

      String ip = httpRequest != null ? httpRequest.getRemoteAddr() : null;
      String location = geoLocationService.getLocationFromIp(ip);
      String deviceFingerprint = deviceFingerprintUtil.generateFingerprint(httpRequest);
      sessionService.createSession(user, accessToken, httpRequest, location, deviceFingerprint);

      String deviceInfo = httpRequest != null ? httpRequest.getHeader("User-Agent") : null;

      if (loginAttemptService.isNewDevice(user.getUsername(), deviceInfo)) {
        securityClient.createAlert(user.getUsername(),
            "NEW_DEVICE_LOGIN",
            "New Device Detected",
            "Login detected from a new device or browser: " + deviceInfo,
            "MEDIUM");
      }

      securityClient.logAudit(user.getUsername(),
          "LOGIN",
          "Successful login (2FA)", ip);
      loginAttemptService.recordLoginAttempt(user.getUsername(), true, null, ip, deviceInfo, location, 0);

      return AuthResponse.builder()
          .accessToken(accessToken)
          .refreshToken(refreshToken)
          .username(user.getUsername())
          .expiresIn(jwtConfig.getAccessTokenExpiration())
          .requires2FA(false)
          .build();
    } catch (Exception e) {
      throw new AuthenticationException(
          "2FA verified but login failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
    }
  }
}
