package com.blogweb.modules.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blogweb.common.BusinessException;
import com.blogweb.config.MainUserProperties;
import com.blogweb.modules.user.UserEntity;
import com.blogweb.modules.user.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String SESSION_USER_ID = "LOGIN_USER_ID";

    private final UserMapper userMapper;
    private final MainUserProperties mainUserProperties;
    private final PasswordEncoder passwordEncoder;
    private final HttpServletRequest request;

    public AuthUserVO register(RegisterRequest registerRequest) {
        UserEntity exists = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, registerRequest.getUsername()));
        if (exists != null) {
            throw new BusinessException("用户名已存在");
        }

        UserEntity user = new UserEntity();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        request.getSession(true).setAttribute(SESSION_USER_ID, user.getId());
        return toVO(user);
    }

    public AuthUserVO login(LoginRequest loginRequest) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, loginRequest.getUsername()));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("密码错误");
        }

        request.getSession(true).setAttribute(SESSION_USER_ID, user.getId());
        return toVO(user);
    }

    public void logout() {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public AuthUserVO currentUser() {
        Long userId = currentUserId();
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toVO(user);
    }

    public AuthUserVO mainUser() {
        UserEntity user = userMapper.selectById(mainUserProperties.getId());
        if (user == null) {
            throw new BusinessException("主用户不存在");
        }
        return toVO(user);
    }

    public Long currentUserId() {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new BusinessException("请先登录");
        }
        Object value = session.getAttribute(SESSION_USER_ID);
        if (!(value instanceof Long userId)) {
            throw new BusinessException("请先登录");
        }
        return userId;
    }

    public Optional<Long> currentUserIdOptional() {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }
        Object value = session.getAttribute(SESSION_USER_ID);
        if (!(value instanceof Long userId)) {
            return Optional.empty();
        }
        return Optional.of(userId);
    }

    private AuthUserVO toVO(UserEntity user) {
        return new AuthUserVO(user.getId(), user.getUsername(), user.getEmail(), user.getAvatar(), user.getBio());
    }
}
