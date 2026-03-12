package com.revature.security.repository;

import com.revature.security.model.SecurityAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {
    List<SecurityAlert> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<SecurityAlert> findByUserIdAndIsReadFalse(Long userId);
    long countByUserIdAndIsReadFalse(Long userId);
}
