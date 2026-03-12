package com.revature.security.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReusedPasswordResponse {

    private int totalReusedGroups;
    private int totalAffectedEntries;
    private List<ReusedPasswordGroup> reusedGroups;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReusedPasswordGroup {
        private int reuseCount;
        private List<ReusedEntryInfo> entries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReusedEntryInfo {
        private Long entryId;
        private String title;
        private String username;
        private String websiteUrl;
    }
}

