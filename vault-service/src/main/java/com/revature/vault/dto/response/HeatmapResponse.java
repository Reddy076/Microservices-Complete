package com.revature.vault.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapResponse {
    private List<Integer> accessByHour;
    private List<Integer> accessByDay;
    private Integer peakHour;
    private String peakDay;
    private int totalAccesses;
    private String period;
}
