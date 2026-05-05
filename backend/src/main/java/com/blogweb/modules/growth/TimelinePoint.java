package com.blogweb.modules.growth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimelinePoint {

    private String date;
    private long count;
}
