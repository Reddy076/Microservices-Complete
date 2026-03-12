package com.revature.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "captcha")
public class CaptchaConfig {

  private boolean enabled = false;
  private String secretKey = "";
  private String verifyUrl = "https://www.google.com/recaptcha/api/siteverify";
  private int failedAttemptsThreshold = 3;
}
