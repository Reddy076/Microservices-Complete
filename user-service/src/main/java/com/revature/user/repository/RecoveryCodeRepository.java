package com.revature.user.repository;

import com.revature.user.model.user.RecoveryCode;
import com.revature.user.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, Long> {
  List<RecoveryCode> findByUser(User user);

  boolean existsByCodeHash(String codeHash);
}
