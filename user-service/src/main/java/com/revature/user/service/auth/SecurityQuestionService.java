package com.revature.user.service.auth;

import com.revature.user.dto.SecurityQuestionDTO;
import com.revature.user.model.user.SecurityQuestion;
import com.revature.user.model.user.User;
import com.revature.user.repository.SecurityQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecurityQuestionService {

  private final SecurityQuestionRepository securityQuestionRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public void saveSecurityQuestions(User user, List<SecurityQuestionDTO> questions) {
    if (questions == null || questions.isEmpty()) {
      return;
    }

    securityQuestionRepository.deleteAllByUserId(user.getId());

    if (questions == null || questions.isEmpty()) {
      return;
    }

    List<SecurityQuestion> securityQuestions = questions.stream()
        .map(dto -> SecurityQuestion.builder()
            .user(user)
            .questionText(dto.getQuestion())
            .answerHash(hashAnswer(dto.getAnswer()))
            .build())
        .collect(Collectors.toList());

    securityQuestionRepository.saveAll(securityQuestions);
  }

  @Transactional(readOnly = true)
  public boolean verifySecurityAnswers(User user, List<SecurityQuestionDTO> providedAnswers) {
    List<SecurityQuestion> storedQuestions = securityQuestionRepository.findAllByUserId(user.getId());

    if (storedQuestions.isEmpty() || providedAnswers == null || providedAnswers.size() != storedQuestions.size()) {
      return false;
    }

    for (SecurityQuestionDTO dto : providedAnswers) {
      boolean matchFound = storedQuestions.stream()
          .filter(sq -> sq.getQuestionText().equals(dto.getQuestion()))
          .anyMatch(sq -> passwordEncoder.matches(normalize(dto.getAnswer()), sq.getAnswerHash()));

      if (!matchFound) {
        return false;
      }
    }

    return true;
  }

  public List<SecurityQuestion> getSecurityQuestions(User user) {
    return securityQuestionRepository.findAllByUserId(user.getId());
  }

  private String hashAnswer(String answer) {
    return passwordEncoder.encode(normalize(answer));
  }

  private String normalize(String input) {
    return input == null ? "" : input.trim().toLowerCase();
  }
}
