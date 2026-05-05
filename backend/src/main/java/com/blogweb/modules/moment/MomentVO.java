package com.blogweb.modules.moment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MomentVO {

    private Long id;
    private String content;
    private String mood;
    private String visibilityScope;
    private Long likeCount;
    private Long commentCount;
    private Boolean likedByCurrentUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
