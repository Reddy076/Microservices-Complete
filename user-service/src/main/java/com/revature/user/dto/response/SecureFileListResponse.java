package com.revature.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Feature 40 – Secure File Storage Vault.
 *
 * <p>Response for {@code GET /api/files} and {@code GET /api/files/search}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecureFileListResponse {

    /** Folders at the current level. */
    private List<FolderItem> folders;

    /** Files at the current level. */
    private List<FileItem> files;

    /** Total number of files across all folders for this user. */
    private long totalFiles;

    /** Total storage used in bytes. */
    private long totalStorageBytes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FolderItem {
        private Long id;
        private String name;
        private Long parentId;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileItem {
        private Long id;
        private String originalFilename;
        private Long fileSize;
        private String mimeType;
        private Long folderId;
        private String folderName;
        private LocalDateTime uploadedAt;
        private LocalDateTime lastAccessedAt;
    }
}
