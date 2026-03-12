package com.revature.vault.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Feature 39 – Emergency Access (Digital Legacy).
 *
 * <p>Response for emergency contact CRUD operations.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContactResponse {

    private Long id;
    private String contactEmail;
    private String contactName;
    private String relationship;
    private Integer waitingPeriodHours;
    private Boolean verified;
    private Boolean active;
    private LocalDateTime createdAt;
}
