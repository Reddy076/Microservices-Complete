package com.revature.security.repository;

import com.revature.security.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserIdOrderByTimestampDesc(Long userId);
    List<AuditLog> findByAction(AuditLog.AuditAction action);
    List<AuditLog> findByUserIdAndTimestampAfter(Long userId, LocalDateTime timestamp);
}
