package com.revature.security.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaultEntryResponse {
    private Long id;
    private String title;
    private String websiteUrl;
    private Long categoryId;
    private String categoryName;
    private Boolean isFavorite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer strengthScore;
    private String strengthLabel;
}
