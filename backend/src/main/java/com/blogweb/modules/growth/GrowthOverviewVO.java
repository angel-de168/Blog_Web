package com.blogweb.modules.growth;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GrowthOverviewVO {

    private List<TimelinePoint> timeline;
    private List<SkillPoint> skills;
    private List<WeekdayPoint> weekday;
    private List<StatusPoint> statuses;
    private GrowthSummaryVO summary;
    private LoopSummaryVO loopSummary;
    private List<LoopTimelinePointVO> loopTimeline;
}
