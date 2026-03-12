package com.revature.vault.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Feature 42 – Team/Family Vault Sharing.
 *
 * <p>Request body for {@code POST /api/teams/{id}/members} (invite by email).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteMemberRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    /** Role to assign: ADMIN, MEMBER, or VIEWER (default MEMBER). */
    private String role;
}
