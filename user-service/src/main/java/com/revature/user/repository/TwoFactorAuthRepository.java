package com.revature.user.repository;

import com.revature.user.model.auth.TwoFactorAuth;
import com.revature.user.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TwoFactorAuthRepository extends JpaRepository<TwoFactorAuth, Long> {
  Optional<TwoFactorAuth> findByUser(User user);
}
