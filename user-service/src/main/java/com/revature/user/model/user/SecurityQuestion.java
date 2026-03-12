package com.revature.user.model.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "security_questions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityQuestion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "question_text", nullable = false)
  private String questionText;

  @Column(name = "answer_hash", nullable = false)
  private String answerHash;
}
