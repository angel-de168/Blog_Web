package com.blogweb.modules.site;

import com.blogweb.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/site")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @GetMapping("/stats")
    public ApiResponse<SiteStatsVO> stats() {
        return ApiResponse.ok(siteService.stats());
    }
}
