package com.blogweb.modules.growth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoopTimelinePointVO {

    private String date;
    private long outputCount;
    private long feedbackCount;
    private long iterationCount;
}
