package com.revature.vault.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Feature 42 – Team/Family Vault Sharing.
 *
 * <p>Response for team CRUD operations and team listing.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {

    private Long id;
    private String name;
    private String description;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private int memberCount;
    private int sharedEntryCount;

    /** The authenticated user's role in this team. */
    private String currentUserRole;

    /** Members list (included when fetching a single team). */
    private List<TeamMemberResponse> members;
}
