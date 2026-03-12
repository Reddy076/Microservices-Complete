package com.revature.user.service.auth;

import com.revature.user.dto.response.TwoFactorSetupResponse;
import com.revature.user.exception.AuthenticationException;
import com.revature.user.model.auth.TwoFactorAuth;
import com.revature.user.model.user.User;
import com.revature.user.repository.TwoFactorAuthRepository;
import com.revature.user.repository.UserRepository;
import com.revature.user.client.SecurityClient;
import com.revature.user.util.TOTPUtil;
import dev.samstevens.totp.exceptions.QrGenerationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class TwoFactorService {

  private final TwoFactorAuthRepository twoFactorAuthRepository;
  private final UserRepository userRepository;
  private final TOTPUtil totpUtil;
  private final OtpService otpService;
  private final SecurityClient securityClient;

  private static final SecureRandom secureRandom = new SecureRandom();
  private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

  @Transactional
  public TwoFactorSetupResponse setup2FA(User user) {
    TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
        .orElse(TwoFactorAuth.builder().user(user).build());

    String secretKey = totpUtil.generateSecret();
    twoFactorAuth.setSecretKey(secretKey);
    twoFactorAuth.setEnabled(false);

    twoFactorAuthRepository.save(twoFactorAuth);

    try {
      String qrCodeUrl = totpUtil.getQrCodeUrl(secretKey, user.getEmail());
      return TwoFactorSetupResponse.builder()
          .secretKey(secretKey)
          .qrCodeUrl(qrCodeUrl)
          .build();
    } catch (QrGenerationException e) {
      throw new AuthenticationException("Failed to generate QR code for 2FA setup");
    }
  }

  @Transactional
  public void verifySetup(User user, String code) {
    TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
        .orElseThrow(() -> new AuthenticationException("2FA setup not initiated"));

    if (totpUtil.verifyCode(twoFactorAuth.getSecretKey(), code)) {
      twoFactorAuth.setEnabled(true);
      twoFactorAuth.setRecoveryCodes(generateRecoveryCodes());
      twoFactorAuthRepository.save(twoFactorAuth);

      user.set2faEnabled(true);
      userRepository.save(user);

      securityClient.createAlert(user.getUsername(),
          "TWO_FA_ENABLED",
          "Two-Factor Authentication Enabled",
          "2FA has been successfully enabled for your account",
          "LOW");
    } else {
      throw new AuthenticationException("Invalid 2FA code");
    }
  }

  @Transactional
  public void disable2FA(User user) {
    TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
        .orElseThrow(() -> new AuthenticationException("2FA not set up for this user"));

    user.set2faEnabled(false);
    userRepository.save(user);

    twoFactorAuthRepository.delete(twoFactorAuth);

    securityClient.createAlert(user.getUsername(),
        "TWO_FA_DISABLED",
        "Two-Factor Authentication Disabled",
        "2FA has been disabled for your account. If you did not make this change, secure your account immediately.",
        "HIGH");
  }

  @Transactional
  public boolean verifyCode(User user, String code) {

    TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
        .orElseThrow(() -> new AuthenticationException("2FA not set up"));

    if (!twoFactorAuth.isEnabled()) {
      return false;
    }

    if (totpUtil.verifyCode(twoFactorAuth.getSecretKey(), code)) {
      return true;
    }

    if (twoFactorAuth.getRecoveryCodes().contains(code)) {

      twoFactorAuth.getRecoveryCodes().remove(code);
      twoFactorAuthRepository.save(twoFactorAuth);
      return true;
    }

    return false;
  }

  @Transactional(readOnly = true)
  public List<String> getRecoveryCodes(User user) {
    TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
        .orElseThrow(() -> new AuthenticationException("2FA not set up"));

    return twoFactorAuth.getRecoveryCodes();
  }

  @Transactional
  public List<String> regenerateRecoveryCodes(User user) {
    TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
        .orElseThrow(() -> new AuthenticationException("2FA not set up"));

    if (!twoFactorAuth.isEnabled()) {
      throw new AuthenticationException("Cannot regenerate codes for unverified 2FA setup");
    }

    List<String> newCodes = generateRecoveryCodes();
    twoFactorAuth.setRecoveryCodes(newCodes);
    twoFactorAuthRepository.save(twoFactorAuth);

    securityClient.createAlert(user.getUsername(),
        "PASSWORD_CHANGED",
        "Backup Codes Regenerated",
        "Your 2FA backup codes have been regenerated. Your old codes will no longer work.",
        "MEDIUM");

    return newCodes;
  }

  private List<String> generateRecoveryCodes() {
    return IntStream.range(0, 10)
        .mapToObj(i -> generateRecoveryCode())
        .collect(Collectors.toList());
  }

  private String generateRecoveryCode() {
    StringBuilder sb = new StringBuilder(10);
    for (int i = 0; i < 10; i++) {
      sb.append(BASE32_ALPHABET.charAt(secureRandom.nextInt(BASE32_ALPHABET.length())));
    }
    return sb.toString();
  }
}
