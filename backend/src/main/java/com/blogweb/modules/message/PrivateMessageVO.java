package com.blogweb.modules.message;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PrivateMessageVO {

    private Long id;
    private Long sessionId;
    private Long senderUserId;
    private String senderUsername;
    private String senderAvatar;
    private String content;
    private LocalDateTime createdAt;
}
