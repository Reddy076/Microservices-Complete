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


  @Transactional
  public void setDuressPassword(String username, String duressPassword) {
    User user = userRepository.findByUsernameOrThrow(username);
    user.setDuressPasswordHash(passwordEncoder.encode(duressPassword));
    userRepository.save(user);
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
