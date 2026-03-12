package com.revature.security.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_metrics_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityMetricsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "overall_score")
    private int overallScore;

    @Column(name = "weak_passwords_count")
    private int weakPasswordsCount;

    @Column(name = "reused_passwords_count")
    private int reusedPasswordsCount;

    @Column(name = "old_passwords_count")
    private int oldPasswordsCount;

    @Column(name = "strong_passwords_count")
    private int strongPasswordsCount;

    @Column(name = "fair_passwords_count")
    private int fairPasswordsCount;

    @Column(name = "total_passwords_count")
    private int totalPasswordsCount;

    @CreationTimestamp
    @Column(name = "recorded_at", updatable = false)
    private LocalDateTime recordedAt;
}


