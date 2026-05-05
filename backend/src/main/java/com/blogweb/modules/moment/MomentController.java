package com.blogweb.modules.moment;

import com.blogweb.common.ApiResponse;
import com.blogweb.common.PagedResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/moments")
@RequiredArgsConstructor
public class MomentController {

    private final MomentService momentService;

    @GetMapping
    public ApiResponse<PagedResult<MomentVO>> list(@RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String mood,
                                                    @RequestParam(required = false) String contentKeyword,
                                                    @RequestParam(required = false) String visibilityScope,
                                                    @RequestParam(required = false) String startTime,
                                                    @RequestParam(required = false) String endTime,
                                                    @RequestParam(required = false) Long minLikeCount,
                                                    @RequestParam(required = false) Long maxLikeCount,
                                                    @RequestParam(defaultValue = "updatedAt") String sortBy,
                                                    @RequestParam(defaultValue = "desc") String sortOrder,
                                                    @RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(momentService.list(keyword, mood, contentKeyword, visibilityScope, startTime, endTime, minLikeCount, maxLikeCount, sortBy, sortOrder, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<MomentVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(momentService.detail(id));
    }

    @PostMapping
    public ApiResponse<MomentVO> create(@Valid @RequestBody SaveMomentRequest request) {
        request.setId(null);
        return ApiResponse.ok(momentService.save(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<MomentVO> update(@PathVariable Long id, @Valid @RequestBody SaveMomentRequest request) {
        request.setId(id);
        return ApiResponse.ok(momentService.save(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        momentService.remove(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/like")
    public ApiResponse<MomentVO> like(@PathVariable Long id) {
        return ApiResponse.ok(momentService.like(id));
    }

    @DeleteMapping("/{id}/like")
    public ApiResponse<MomentVO> unlike(@PathVariable Long id) {
        return ApiResponse.ok(momentService.unlike(id));
    }

    @GetMapping("/{id}/comments")
    public ApiResponse<PagedResult<MomentCommentVO>> comments(@PathVariable Long id,
                                                               @RequestParam(defaultValue = "1") long page,
                                                               @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(momentService.listComments(id, page, size));
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<MomentCommentVO> createComment(@PathVariable Long id,
                                                       @Valid @RequestBody SaveMomentCommentRequest request) {
        return ApiResponse.ok(momentService.addComment(id, request));
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long id, @PathVariable Long commentId) {
        momentService.removeComment(id, commentId);
        return ApiResponse.ok(null);
    }
}
