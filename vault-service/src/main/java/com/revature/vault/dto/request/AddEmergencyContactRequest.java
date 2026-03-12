package com.revature.vault.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Feature 39 – Emergency Access (Digital Legacy).
 *
 * <p>Request body for {@code POST /api/emergency/contacts} (add) and
 * {@code PUT /api/emergency/contacts/{id}} (update).
 * For add, {@code contactEmail} is required and validated in the service.
 * For update, {@code contactEmail} is ignored (email cannot be changed).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddEmergencyContactRequest {

    /** Required for add; ignored for update. Must be a valid email if provided. */
    @Email(message = "Contact email must be a valid email address")
    private String contactEmail;

    private String contactName;

    private String relationship;

    /**
     * Waiting period in hours before access is auto-granted if the owner does not deny.
     * Must be between 1 hour and 720 hours (30 days). Default 48 hours.
     */
    @Min(value = 1, message = "Waiting period must be at least 1 hour")
    @Max(value = 720, message = "Waiting period must not exceed 720 hours (30 days)")
    private Integer waitingPeriodHours;
}
