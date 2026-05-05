package com.blogweb.modules.moment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SaveMomentCommentRequest {

    @NotBlank(message = "评论内容必填")
    private String content;
}
