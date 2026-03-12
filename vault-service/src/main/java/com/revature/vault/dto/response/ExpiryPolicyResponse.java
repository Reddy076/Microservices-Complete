package com.revature.vault.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Feature 38 – Password Expiration Tracker.
 *
 * <p>Response for {@code GET /api/expiry/policy} and {@code PUT /api/expiry/policy}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpiryPolicyResponse {

    private Long id;
    private Integer defaultExpiryDays;
    private Integer criticalExpiryDays;
    private Integer reminderDaysBefore;
    private Boolean enabled;
}
