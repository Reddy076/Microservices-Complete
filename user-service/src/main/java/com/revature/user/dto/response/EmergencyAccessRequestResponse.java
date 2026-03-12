package com.revature.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Feature 39 – Emergency Access (Digital Legacy).
 *
 * <p>Response for emergency access request operations.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyAccessRequestResponse {

    private Long id;

    /** ID of the emergency contact who made the request. */
    private Long contactId;

    /** Email of the emergency contact. */
    private String contactEmail;

    /** Name of the emergency contact. */
    private String contactName;

    /** Username of the vault owner. */
    private String ownerUsername;

    /** Current status: PENDING / APPROVED / DENIED / EXPIRED. */
    private String status;

    private LocalDateTime requestedAt;

    /** When the waiting period ends (auto-approval time). */
    private LocalDateTime waitingPeriodEndsAt;

    /** Hours remaining in the waiting period (negative = already elapsed). */
    private long hoursUntilAutoApproval;

    private LocalDateTime decidedAt;

    /** Access token (only present when status = APPROVED). */
    private String accessToken;

    /** When the access token expires. */
    private LocalDateTime expiresAt;

    private String requestMessage;
}
