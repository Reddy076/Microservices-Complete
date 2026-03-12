package com.revature.notification.repository;

import com.revature.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

  List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

  long countByUserIdAndIsReadFalse(Long userId);

  Optional<Notification> findByIdAndUserId(Long id, Long userId);
}
