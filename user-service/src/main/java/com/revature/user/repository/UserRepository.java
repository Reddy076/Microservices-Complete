package com.revature.user.repository;

import com.revature.user.exception.ResourceNotFoundException;
import com.revature.user.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  default User findByUsernameOrThrow(String username) {
    return findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
  }

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  List<User> findByDeletionScheduledAtBefore(LocalDateTime dateTime);

  /** Returns all active users — those NOT pending deletion. */
  List<User> findByDeletionScheduledAtIsNull();

  @Modifying(clearAutomatically = true)
  @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.username = :username OR u.email = :username")
  void incrementFailedLoginAttempts(@Param("username") String username);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = null WHERE u.username = :username OR u.email = :username")
  void resetFailedLoginAttempts(@Param("username") String username);
}
