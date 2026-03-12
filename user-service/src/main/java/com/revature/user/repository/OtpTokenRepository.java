package com.revature.user.repository;

import com.revature.user.model.auth.OtpToken;
import com.revature.user.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
  Optional<OtpToken> findByToken(String token);

  List<OtpToken> findByUserAndTokenType(User user, String tokenType);

  void deleteByExpiryDateBefore(LocalDateTime dateTime);
}
