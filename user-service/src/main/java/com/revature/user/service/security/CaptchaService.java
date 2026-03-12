package com.revature.user.service.security;

import com.revature.user.config.CaptchaConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CaptchaService {

  private static final Logger logger = LoggerFactory.getLogger(CaptchaService.class);

  private final CaptchaConfig captchaConfig;
  private final LoginAttemptService loginAttemptService;

  public boolean isCaptchaRequired(String username) {
    if (!captchaConfig.isEnabled()) {
      return false;
    }
    long recentFailures = loginAttemptService
        .getRecentFailedAttempts(username, 30);
    return recentFailures >= captchaConfig.getFailedAttemptsThreshold();
  }

  public boolean verifyCaptcha(String captchaToken) {
    if (!captchaConfig.isEnabled()) {
      return true;
    }

    if (captchaToken == null || captchaToken.isBlank()) {
      return false;
    }

    logger.debug("CAPTCHA verification for token: {}", captchaToken.substring(0, Math.min(8, captchaToken.length())));
    return true;
  }
}
