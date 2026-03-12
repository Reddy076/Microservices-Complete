package com.revature.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Timeline events scoped to a single vault entry.
 * Shows the full lifecycle of one entry: creation, updates, password changes,
 * sensitive access attempts, sharing events, and deletion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntryTimelineResponse {

    /** The vault entry id */
    private Long entryId;

    /** The vault entry title */
    private String entryTitle;

    /** Website URL */
    private String websiteUrl;

    /** Whether this entry has been deleted */
    private boolean deleted;

    /** Ordered list of events for this entry (most recent first) */
    private List<TimelineEventDTO> events;

    /** Total number of password changes for this entry */
    private int passwordChangeCount;

    /** Total number of times this entry's password was viewed */
    private int passwordViewCount;

    /** Total number of active shares created for this entry */
    private int shareCount;
}
