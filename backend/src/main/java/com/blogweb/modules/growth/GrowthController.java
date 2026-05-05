package com.blogweb.modules.growth;

import com.blogweb.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/growth")
@RequiredArgsConstructor
public class GrowthController {

    private final GrowthService growthService;

    @GetMapping("/overview")
    public ApiResponse<GrowthOverviewVO> overview(@RequestParam(defaultValue = "120") int days) {
        return ApiResponse.ok(growthService.overview(days));
    }
}
