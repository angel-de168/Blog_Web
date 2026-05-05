package com.blogweb.modules.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendPrivateMessageRequest {

    @NotBlank(message = "消息内容必填")
    @Size(max = 1000, message = "消息内容不能超过1000字")
    private String content;
}
