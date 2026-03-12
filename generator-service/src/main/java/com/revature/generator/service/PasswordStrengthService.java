package com.revature.generator.service;

import com.revature.generator.dto.response.PasswordStrengthResponse;
import com.revature.generator.util.PasswordStrengthCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PasswordStrengthService {

  private final PasswordStrengthCalculator calculator;

  public PasswordStrengthResponse analyzePassword(String password) {
    int score = calculator.calculateScore(password);
    String label = calculator.getStrengthLabel(score);
    List<String> feedback = generateFeedback(password, score);

    return PasswordStrengthResponse.builder()
        .score(score)
        .label(label)
        .feedback(feedback)
        .build();
  }

  private List<String> generateFeedback(String password, int score) {
    List<String> feedback = new ArrayList<>();
    if (password == null || password.isEmpty()) {
      feedback.add("Password is empty");
      return feedback;
    }

    if (password.length() < 8) {
      feedback.add("Password is too short (minimum 8 characters recommended)");
    }
    if (password.chars().noneMatch(Character::isUpperCase)) {
      feedback.add("Add uppercase letters");
    }
    if (password.chars().noneMatch(Character::isLowerCase)) {
      feedback.add("Add lowercase letters");
    }
    if (password.chars().noneMatch(Character::isDigit)) {
      feedback.add("Add numbers");
    }
    if (password.chars().allMatch(Character::isLetterOrDigit)) {
      feedback.add("Add special characters");
    }

    if (score < 60) {
      feedback.add("Avoid common patterns or repeated characters");
    }

    return feedback;
  }
}

