package com.blogweb.modules.growth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GrowthSummaryVO {

    private long totalNotes;
    private long activeDays;
    private long peakDayCount;
    private long avgPerWeek;
}
