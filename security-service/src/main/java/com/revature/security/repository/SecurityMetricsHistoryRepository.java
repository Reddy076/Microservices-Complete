package com.revature.security.repository;

import com.revature.security.model.SecurityMetricsHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SecurityMetricsHistoryRepository extends JpaRepository<SecurityMetricsHistory, Long> {

    List<SecurityMetricsHistory> findByUserIdOrderByRecordedAtDesc(Long userId);

    List<SecurityMetricsHistory> findByUserIdAndRecordedAtAfterOrderByRecordedAtAsc(
            Long userId, LocalDateTime after);

    Optional<SecurityMetricsHistory> findTopByUserIdOrderByRecordedAtDesc(Long userId);

    @Query("SELECT s FROM SecurityMetricsHistory s WHERE s.userId = :userId " +
           "AND s.recordedAt >= :since ORDER BY s.recordedAt ASC")
    List<SecurityMetricsHistory> findTrendData(@Param("userId") Long userId,
                                               @Param("since") LocalDateTime since);

    void deleteByUserId(Long userId);
}
