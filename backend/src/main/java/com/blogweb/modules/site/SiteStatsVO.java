package com.blogweb.modules.site;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SiteStatsVO {

    private Long noteCount;
    private Long tagCount;
    private Long visitCount;
}
