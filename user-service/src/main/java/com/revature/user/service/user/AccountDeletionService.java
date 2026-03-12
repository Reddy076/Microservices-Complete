package com.revature.user.service.user;

import com.revature.user.dto.request.AccountDeletionRequest;
import com.revature.user.exception.AuthenticationException;
import com.revature.user.model.user.User;
import com.revature.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountDeletionService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public void scheduleAccountDeletion(String username, AccountDeletionRequest request) {
    User user = userRepository.findByUsernameOrThrow(username);

    if (!passwordEncoder.matches(request.getMasterPassword(), user.getMasterPasswordHash())) {
      throw new AuthenticationException("Invalid master password");
    }

    user.setDeletionRequestedAt(LocalDateTime.now());

    user.setDeletionScheduledAt(LocalDateTime.now().plusDays(30));

    userRepository.save(user);
  }

  @Transactional
  public void cancelAccountDeletion(String username) {
    User user = userRepository.findByUsernameOrThrow(username);

    if (user.getDeletionScheduledAt() == null) {
      throw new IllegalArgumentException("Account is not scheduled for deletion");
    }

    user.setDeletionRequestedAt(null);
    user.setDeletionScheduledAt(null);

    userRepository.save(user);
  }
}
