package com.revature.user.repository;

import com.revature.user.model.user.SecurityQuestion;
import com.revature.user.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecurityQuestionRepository extends JpaRepository<SecurityQuestion, Long> {
  @Query("SELECT sq FROM SecurityQuestion sq WHERE sq.user.id = :userId")
  List<SecurityQuestion> findAllByUserId(Long userId);

  @Modifying
  @Query("DELETE FROM SecurityQuestion sq WHERE sq.user.id = :userId")
  void deleteAllByUserId(Long userId);
}
