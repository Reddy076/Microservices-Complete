package com.revature.user.service.security;

import com.revature.user.model.user.User;
import com.revature.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class DuressService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final com.revature.user.client.SecurityClient securityClient;

  @Transactional
  public void setDuressPassword(String username, String duressPassword) {
    User user = userRepository.findByUsernameOrThrow(username);
    user.setDuressPasswordHash(passwordEncoder.encode(duressPassword));
    userRepository.save(user);

    try {
      securityClient.createAlert(username,
          "DURESS_MODE_ACTIVATED",
          "Duress (Read-Only) Mode Configured",
          "A duress password has been set for your account. Logging in with it will activate read-only mode to protect your data.",
          "MEDIUM");
    } catch (Exception e) { /* non-critical */ }
  }

  public boolean isDuressLogin(String username, String password) {
    User user = userRepository.findByUsername(username)
        .or(() -> userRepository.findByEmail(username))
        .orElse(null);
    if (user == null || user.getDuressPasswordHash() == null) {
      return false;
    }
    return passwordEncoder.matches(password, user.getDuressPasswordHash());
  }
}
