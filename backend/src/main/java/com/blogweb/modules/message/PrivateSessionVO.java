package com.blogweb.modules.message;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PrivateSessionVO {

    private Long id;
    private Long peerUserId;
    private String peerUsername;
    private String peerAvatar;
    private String peerBio;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
}
