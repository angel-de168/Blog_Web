package com.blogweb.modules.note;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class NoteVO {

    private Long id;
    private String title;
    private String content;
    private Long categoryId;
    private String categoryName;
    private String status;
    private List<String> tags;
    private String coverImage;
    private Long readCount;
    private Long commentCount;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
