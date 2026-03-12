package com.revature.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderDTO {
  private Long id;
  private String name;
  private Long parentFolderId;
  private List<FolderDTO> subfolders;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
