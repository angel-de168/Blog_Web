package com.blogweb.modules.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthUserVO {

    private Long id;
    private String username;
    private String email;
    private String avatar;
    private String bio;
}
