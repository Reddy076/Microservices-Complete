package com.revature.vault.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optional request body for the {@code GET /api/shares/{token}} endpoint.
 *
 * <p>Gap analysis (Feature 35) identified that this class was proposed in the spec but
 * was not implemented because the retrieval endpoint accepts the token as a URL path
 * variable and requires no mandatory request body for correct HTTP semantics.</p>
 *
 * <p>This DTO is retained for forward-compatibility: future enhancements may require
 * the recipient to supply additional context (e.g. a one-time access PIN, a recipient
 * identity token, or a decryption hint). For now all fields are optional and the
 * endpoint continues to function without a body.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * POST /api/shares/{token}
 * Content-Type: application/json
 *
 * {
 *   "recipientNote": "Accessing for team setup",
 *   "recipientEmail": "bob@example.com"
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiveShareRequest {

    /**
     * Optional note from the recipient explaining why they are accessing the share.
     * Can be logged for audit purposes. Max 255 characters.
     */
    private String recipientNote;

    /**
     * Optional self-reported email of the recipient.
     * When provided, can be cross-checked against {@link com.revature.vault.model.sharing.SecureShare#getRecipientEmail()}
     * for additional access validation in future implementations.
     */
    private String recipientEmail;
}
