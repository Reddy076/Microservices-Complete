package com.revature.generator.util;

import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {

  public boolean isValid(String password) {

    return password != null && password.length() >= 8;
  }
}

