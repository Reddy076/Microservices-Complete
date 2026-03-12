package com.revature.user.repository;

import com.revature.user.model.security.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

  List<LoginAttempt> findByUserIdOrderByTimestampDesc(Long userId);

  List<LoginAttempt> findByUsernameAttemptedOrderByTimestampDesc(String username);

  long countByUsernameAttemptedAndSuccessfulFalseAndTimestampAfter(String username, LocalDateTime after);

  List<LoginAttempt> findByUserIdAndTimestampBetweenOrderByTimestampDesc(
      Long userId, LocalDateTime start, LocalDateTime end);

  boolean existsByUserIdAndDeviceInfoAndSuccessfulTrue(Long userId, String deviceInfo);
}
