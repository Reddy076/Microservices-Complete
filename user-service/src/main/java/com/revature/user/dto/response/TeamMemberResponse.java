package com.revature.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Feature 42 – Team/Family Vault Sharing.
 *
 * <p>Response for team member operations.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberResponse {

    private Long memberId;
    private Long userId;
    private String username;
    private String email;
    private String role;
    private LocalDateTime joinedAt;
}
