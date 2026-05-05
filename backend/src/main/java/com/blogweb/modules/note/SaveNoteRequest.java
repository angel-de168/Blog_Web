package com.blogweb.modules.note;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class SaveNoteRequest {

    private Long id;

    @NotBlank(message = "标题必填")
    private String title;

    @NotBlank(message = "内容必填")
    private String content;

    private String status;
    private Long categoryId;
    private List<String> tags;
    private String coverImage;
}
