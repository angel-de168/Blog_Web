package com.blogweb.modules.message;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSessionRequest {

    @NotNull(message = "目标用户必填")
    private Long targetUserId;
}
