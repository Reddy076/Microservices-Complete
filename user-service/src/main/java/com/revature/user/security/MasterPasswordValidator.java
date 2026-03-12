package com.revature.user.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class MasterPasswordValidator {

  @SuppressWarnings("java:S2068") // Not a hardcoded password, just a regex pattern validation string
  private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{12,}$";

  private final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

  public boolean isValid(String password) {
    if (password == null) {
      return false;
    }
    return pattern.matcher(password).matches();
  }

  public String getRequirementsMessage() {
    return "Master password must be at least 12 characters long and contain at " +
        "least one digit, one lowercase letter, one uppercase letter, " +
        "and one special character (@#$%^&+=!). Details must not contain whitespace.";
  }
}
