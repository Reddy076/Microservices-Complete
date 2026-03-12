package com.revature.vault.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompromisedCredentialResponse {

    private Long id;
    private Long vaultEntryId;
    private String vaultEntryTitle;
    private String username;
    private String websiteUrl;
    private long pwnedCount;
    private boolean resolved;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
}
