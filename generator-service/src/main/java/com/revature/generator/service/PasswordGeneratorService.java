package com.revature.generator.service;

import com.revature.generator.dto.request.PasswordGeneratorRequest;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PasswordGeneratorService {

  private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
  private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String NUMBERS = "0123456789";
  private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";

  private static final String SIMILAR_CHARS = "il1Lo0O";

  private static final String AMBIGUOUS_CHARS = "{}[]()/\\'\"`~,;:.<>";

  private final SecureRandom random = new SecureRandom();

  public String generatePassword(PasswordGeneratorRequest request) {
    StringBuilder charPool = new StringBuilder();
    List<Character> passwordChars = new ArrayList<>();

    if (request.isIncludeLowercase()) {
      charPool.append(filterChars(LOWERCASE, request));
      passwordChars.add(getRandomChar(filterChars(LOWERCASE, request)));
    }
    if (request.isIncludeUppercase()) {
      charPool.append(filterChars(UPPERCASE, request));
      passwordChars.add(getRandomChar(filterChars(UPPERCASE, request)));
    }
    if (request.isIncludeNumbers()) {
      charPool.append(filterChars(NUMBERS, request));
      passwordChars.add(getRandomChar(filterChars(NUMBERS, request)));
    }
    if (request.isIncludeSpecial()) {
      charPool.append(filterChars(SPECIAL_CHARS, request));
      passwordChars.add(getRandomChar(filterChars(SPECIAL_CHARS, request)));
    }

    if (charPool.length() == 0) {
      throw new IllegalArgumentException("At least one character type must be selected");
    }

    int remainingLength = request.getLength() - passwordChars.size();
    if (remainingLength < 0) {

    } else {
      String pool = charPool.toString();
      for (int i = 0; i < remainingLength; i++) {
        passwordChars.add(pool.charAt(random.nextInt(pool.length())));
      }
    }

    Collections.shuffle(passwordChars, random);

    StringBuilder password = new StringBuilder();
    for (Character c : passwordChars) {
      password.append(c);
    }

    return password.toString();
  }

  private String filterChars(String source, PasswordGeneratorRequest request) {
    StringBuilder sb = new StringBuilder();
    for (char c : source.toCharArray()) {
      boolean skip = false;
      if (request.isExcludeSimilar() && SIMILAR_CHARS.indexOf(c) >= 0) {
        skip = true;
      }
      if (request.isExcludeAmbiguous() && AMBIGUOUS_CHARS.indexOf(c) >= 0) {
        skip = true;
      }
      if (!skip) {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private char getRandomChar(String source) {
    if (source.isEmpty()) {

      return '?';
    }
    return source.charAt(random.nextInt(source.length()));
  }

  public List<String> generateMultiplePasswords(PasswordGeneratorRequest request) {
    List<String> passwords = new ArrayList<>();
    for (int i = 0; i < request.getCount(); i++) {
      passwords.add(generatePassword(request));
    }
    return passwords;
  }

  public PasswordGeneratorRequest getDefaultSettings() {
    return new PasswordGeneratorRequest();
  }
}

