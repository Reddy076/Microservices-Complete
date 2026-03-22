package com.revature.security.repository;

import com.revature.security.model.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    List<LoginAttempt> findByUserIdOrderByTimestampDesc(Long userId);

    @Query("SELECT a FROM LoginAttempt a WHERE a.usernameAttempted = :username ORDER BY a.timestamp DESC")
    List<LoginAttempt> findByUsernameOrderByTimestampDesc(@Param("username") String username);

    @Query("SELECT COUNT(a) FROM LoginAttempt a WHERE a.userId = :userId AND a.successful = false AND a.timestamp > :timestamp")
    long countByUserIdAndIsSuccessfulFalseAndTimestampAfter(@Param("userId") Long userId, @Param("timestamp") LocalDateTime timestamp);

    @Query("SELECT COUNT(a) FROM LoginAttempt a WHERE a.usernameAttempted = :username AND a.successful = false AND a.timestamp > :timestamp")
    long countByUsernameAndIsSuccessfulFalseAndTimestampAfter(@Param("username") String username, @Param("timestamp") LocalDateTime timestamp);
}
