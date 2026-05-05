package com.blogweb.modules.moment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SaveMomentRequest {

    private Long id;

    @NotBlank(message = "内容必填")
    private String content;

    private String mood;

    private String visibilityScope;
}
