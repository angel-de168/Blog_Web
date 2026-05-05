package com.blogweb.modules.note;

import com.blogweb.common.ApiResponse;
import com.blogweb.common.PagedResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @GetMapping
    public ApiResponse<PagedResult<NoteVO>> list(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String contentKeyword,
                                                  @RequestParam(required = false) String startTime,
                                                  @RequestParam(required = false) String endTime,
                                                  @RequestParam(required = false) Long minReadCount,
                                                  @RequestParam(required = false) Long maxReadCount,
                                                  @RequestParam(defaultValue = "updatedAt") String sortBy,
                                                  @RequestParam(defaultValue = "desc") String sortOrder,
                                                  @RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(noteService.list(keyword, status, contentKeyword, startTime, endTime, minReadCount, maxReadCount, sortBy, sortOrder, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<NoteVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(noteService.detail(id));
    }

    @GetMapping("/{id}/comments")
    public ApiResponse<PagedResult<NoteCommentVO>> comments(@PathVariable Long id,
                                                             @RequestParam(defaultValue = "1") long page,
                                                             @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(noteService.listComments(id, page, size));
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<NoteCommentVO> createComment(@PathVariable Long id,
                                                     @Valid @RequestBody SaveNoteCommentRequest request) {
        return ApiResponse.ok(noteService.addComment(id, request));
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long id, @PathVariable Long commentId) {
        noteService.removeComment(id, commentId);
        return ApiResponse.ok(null);
    }

    @PostMapping
    public ApiResponse<NoteVO> create(@Valid @RequestBody SaveNoteRequest request) {
        request.setId(null);
        return ApiResponse.ok(noteService.save(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<NoteVO> update(@PathVariable Long id, @Valid @RequestBody SaveNoteRequest request) {
        request.setId(id);
        return ApiResponse.ok(noteService.save(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        noteService.remove(id);
        return ApiResponse.ok(null);
    }
}
