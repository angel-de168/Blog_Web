package com.blogweb.modules.growth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoopSummaryVO {

    private long notesCreated;
    private long momentsCreated;
    private long totalOutput;
    private long noteCommentsReceived;
    private long momentCommentsReceived;
    private long momentLikesReceived;
    private long totalFeedback;
    private long notesIterated;
    private double iterationCoverage;
    private double avgIterationLagDays;
    private double feedbackPerOutput;
}
