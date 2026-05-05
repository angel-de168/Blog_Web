package com.blogweb.modules.follow;

import com.blogweb.common.ApiResponse;
import com.blogweb.common.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{targetUserId}")
    public ApiResponse<Void> follow(@PathVariable Long targetUserId) {
        followService.follow(targetUserId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{targetUserId}")
    public ApiResponse<Void> unfollow(@PathVariable Long targetUserId) {
        followService.unfollow(targetUserId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/following")
    public ApiResponse<PagedResult<FollowUserVO>> following(@RequestParam(defaultValue = "1") long page,
                                                             @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(followService.following(page, size));
    }

    @GetMapping("/followers")
    public ApiResponse<PagedResult<FollowUserVO>> followers(@RequestParam(defaultValue = "1") long page,
                                                             @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(followService.followers(page, size));
    }

    @GetMapping("/status")
    public ApiResponse<Boolean> status(@RequestParam Long targetUserId) {
        return ApiResponse.ok(followService.status(targetUserId));
    }
}
