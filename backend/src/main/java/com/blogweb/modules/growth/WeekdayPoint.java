package com.blogweb.modules.growth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WeekdayPoint {

    private String label;
    private long count;
}
