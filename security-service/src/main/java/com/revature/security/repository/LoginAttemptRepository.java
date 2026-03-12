package com.revature.security.repository;

import com.revature.security.model.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    List<LoginAttempt> findByUserIdOrderByTimestampDesc(Long userId);
    List<LoginAttempt> findByUsernameOrderByTimestampDesc(String username);
    long countByUserIdAndIsSuccessfulFalseAndTimestampAfter(Long userId, LocalDateTime timestamp);
    long countByUsernameAndIsSuccessfulFalseAndTimestampAfter(String username, LocalDateTime timestamp);
}
