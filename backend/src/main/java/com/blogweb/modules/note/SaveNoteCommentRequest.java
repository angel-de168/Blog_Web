package com.blogweb.modules.note;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SaveNoteCommentRequest {

    @NotBlank(message = "评论内容必填")
    private String content;
}
