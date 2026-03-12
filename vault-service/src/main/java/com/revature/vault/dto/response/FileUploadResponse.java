package com.revature.vault.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Feature 40 – Secure File Storage Vault.
 *
 * <p>Response for {@code POST /api/files/upload}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    private Long id;
    private String originalFilename;
    private Long fileSize;
    private String mimeType;
    private String checksum;
    private Long folderId;
    private String folderName;
    private LocalDateTime uploadedAt;
}
