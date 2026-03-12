package com.revature.user.repository;

import com.revature.user.model.user.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

  List<UserSession> findByUserIdAndIsActiveTrue(Long userId);

  Optional<UserSession> findFirstByTokenOrderByIdDesc(String token);
}
