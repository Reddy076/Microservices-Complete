package com.revature.vault.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class DeviceFingerprintUtil {

  public String generateFingerprint(HttpServletRequest request) {
    if (request == null) {
      return "unknown";
    }

    String userAgent = request.getHeader("User-Agent");
    String acceptLanguage = request.getHeader("Accept-Language");
    String acceptEncoding = request.getHeader("Accept-Encoding");

    StringBuilder fingerprintRaw = new StringBuilder();
    fingerprintRaw.append(userAgent != null ? userAgent : "N/A").append("|");
    fingerprintRaw.append(acceptLanguage != null ? acceptLanguage : "N/A").append("|");
    fingerprintRaw.append(acceptEncoding != null ? acceptEncoding : "N/A");

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(fingerprintRaw.toString().getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {

      return Integer.toHexString(fingerprintRaw.toString().hashCode());
    }
  }
}
