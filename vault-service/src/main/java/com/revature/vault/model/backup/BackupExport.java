package com.revature.vault.model.backup;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "backup_exports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupExport {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name="user_id", nullable=false) private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "export_format", nullable = false, length = 20)
  private ExportFormat exportFormat;

  @Column(name = "file_name", nullable = false, length = 200)
  private String fileName;

  @Column(name = "entry_count")
  private int entryCount;

  @Column(name = "is_encrypted")
  @Builder.Default
  private boolean isEncrypted = true;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public enum ExportFormat {
    JSON, CSV, XML
  }
}



