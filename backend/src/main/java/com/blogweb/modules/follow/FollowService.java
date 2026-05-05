package com.blogweb.modules.follow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blogweb.common.BusinessException;
import com.blogweb.common.PagedResult;
import com.blogweb.modules.auth.AuthService;
import com.blogweb.modules.user.UserEntity;
import com.blogweb.modules.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;
    private final AuthService authService;

    public void follow(Long targetUserId) {
        Long userId = authService.currentUserId();
        UserEntity target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new BusinessException("用户不存在");
        }
        if (userId.equals(targetUserId)) {
            throw new BusinessException("不能关注自己");
        }

        FollowEntity exists = followMapper.selectOne(new LambdaQueryWrapper<FollowEntity>()
                .eq(FollowEntity::getFollowerUserId, userId)
                .eq(FollowEntity::getFollowingUserId, targetUserId)
                .last("limit 1"));
        if (exists != null) {
            return;
        }

        FollowEntity entity = new FollowEntity();
        entity.setFollowerUserId(userId);
        entity.setFollowingUserId(targetUserId);
        entity.setCreatedAt(LocalDateTime.now());
        followMapper.insert(entity);
    }

    public void unfollow(Long targetUserId) {
        Long userId = authService.currentUserId();
        if (userId.equals(targetUserId)) {
            throw new BusinessException("不能取关自己");
        }

        followMapper.delete(new LambdaQueryWrapper<FollowEntity>()
                .eq(FollowEntity::getFollowerUserId, userId)
                .eq(FollowEntity::getFollowingUserId, targetUserId));
    }

    public PagedResult<FollowUserVO> following(long page, long size) {
        Long userId = authService.currentUserId();

        Page<FollowEntity> data = followMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<FollowEntity>()
                        .eq(FollowEntity::getFollowerUserId, userId)
                        .orderByDesc(FollowEntity::getCreatedAt)
        );

        List<FollowUserVO> records = data.getRecords().stream()
                .map(item -> toFollowUserVO(item.getFollowingUserId()))
                .toList();

        return new PagedResult<>(records, data.getTotal(), data.getCurrent(), data.getSize());
    }

    public PagedResult<FollowUserVO> followers(long page, long size) {
        Long userId = authService.currentUserId();

        Page<FollowEntity> data = followMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<FollowEntity>()
                        .eq(FollowEntity::getFollowingUserId, userId)
                        .orderByDesc(FollowEntity::getCreatedAt)
        );

        List<FollowUserVO> records = data.getRecords().stream()
                .map(item -> toFollowUserVO(item.getFollowerUserId()))
                .toList();

        return new PagedResult<>(records, data.getTotal(), data.getCurrent(), data.getSize());
    }

    public boolean status(Long targetUserId) {
        if (targetUserId == null || targetUserId <= 0) {
            throw new BusinessException("目标用户无效");
        }
        Long userId = authService.currentUserIdOptional().orElse(null);
        if (userId == null || userId.equals(targetUserId)) {
            return false;
        }

        Long count = followMapper.selectCount(new LambdaQueryWrapper<FollowEntity>()
                .eq(FollowEntity::getFollowerUserId, userId)
                .eq(FollowEntity::getFollowingUserId, targetUserId));
        return count != null && count > 0;
    }

    private FollowUserVO toFollowUserVO(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            return FollowUserVO.builder()
                    .id(userId)
                    .username("用户")
                    .avatar(null)
                    .bio(null)
                    .build();
        }
        return FollowUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .build();
    }
}
