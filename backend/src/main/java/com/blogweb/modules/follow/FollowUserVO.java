package com.blogweb.modules.follow;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FollowUserVO {

    private Long id;
    private String username;
    private String avatar;
    private String bio;
}
