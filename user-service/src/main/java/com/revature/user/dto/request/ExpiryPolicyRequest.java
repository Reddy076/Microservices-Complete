package com.revature.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Feature 38 – Password Expiration Tracker.
 *
 * <p>Request body for {@code PUT /api/expiry/policy}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpiryPolicyRequest {

    /** Days until a password is considered expired. Must be between 1 and 3650 (10 years). */
    @Min(value = 1, message = "defaultExpiryDays must be at least 1")
    @Max(value = 3650, message = "defaultExpiryDays must not exceed 3650")
    private Integer defaultExpiryDays;

    /** Days until a password is considered critically old. Must be >= defaultExpiryDays. */
    @Min(value = 1, message = "criticalExpiryDays must be at least 1")
    @Max(value = 3650, message = "criticalExpiryDays must not exceed 3650")
    private Integer criticalExpiryDays;

    /** How many days before expiry to send a reminder. */
    @Min(value = 1, message = "reminderDaysBefore must be at least 1")
    @Max(value = 365, message = "reminderDaysBefore must not exceed 365")
    private Integer reminderDaysBefore;

    /** Whether expiry tracking is enabled. */
    private Boolean enabled;
}
