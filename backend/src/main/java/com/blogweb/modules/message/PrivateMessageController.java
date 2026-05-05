package com.blogweb.modules.message;

import com.blogweb.common.ApiResponse;
import com.blogweb.common.PagedResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class PrivateMessageController {

    private final PrivateMessageService privateMessageService;

    @PostMapping("/sessions")
    public ApiResponse<PrivateSessionVO> createOrGetSession(@Valid @RequestBody CreateSessionRequest request) {
        return ApiResponse.ok(privateMessageService.createOrGetSession(request.getTargetUserId()));
    }

    @GetMapping("/sessions")
    public ApiResponse<PagedResult<PrivateSessionVO>> sessions(@RequestParam(defaultValue = "1") long page,
                                                                @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(privateMessageService.sessions(page, size));
    }

    @GetMapping("/sessions/{sessionId}/items")
    public ApiResponse<PagedResult<PrivateMessageVO>> messages(@PathVariable Long sessionId,
                                                                @RequestParam(defaultValue = "1") long page,
                                                                @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(privateMessageService.messages(sessionId, page, size));
    }

    @PostMapping("/sessions/{sessionId}/items")
    public ApiResponse<PrivateMessageVO> send(@PathVariable Long sessionId,
                                              @Valid @RequestBody SendPrivateMessageRequest request) {
        return ApiResponse.ok(privateMessageService.send(sessionId, request));
    }
}
