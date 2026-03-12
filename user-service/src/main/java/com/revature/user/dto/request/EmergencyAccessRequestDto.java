package com.revature.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Feature 39 – Emergency Access (Digital Legacy).
 *
 * <p>Request body for {@code POST /api/emergency/request-access}.
 * The contact provides the vault owner's username and an optional message.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyAccessRequestDto {

    /**
     * Username of the vault owner whose vault the contact wants to access.
     * The contact must already be listed as an emergency contact for this user.
     */
    private String ownerUsername;

    /** Optional message explaining the reason for the request. */
    private String requestMessage;
}
