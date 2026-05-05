package com.blogweb.modules.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blogweb.common.BusinessException;
import com.blogweb.common.PagedResult;
import com.blogweb.modules.auth.AuthService;
import com.blogweb.modules.user.UserEntity;
import com.blogweb.modules.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrivateMessageService {

    private final PrivateSessionMapper privateSessionMapper;
    private final PrivateMessageMapper privateMessageMapper;
    private final UserMapper userMapper;
    private final AuthService authService;

    public PrivateSessionVO createOrGetSession(Long targetUserId) {
        Long currentUserId = authService.currentUserId();
        if (targetUserId == null || targetUserId <= 0) {
            throw new BusinessException("目标用户无效");
        }
        if (currentUserId.equals(targetUserId)) {
            throw new BusinessException("不能给自己发私信");
        }

        UserEntity targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException("用户不存在");
        }

        long userAId = Math.min(currentUserId, targetUserId);
        long userBId = Math.max(currentUserId, targetUserId);

        PrivateSessionEntity exists = privateSessionMapper.selectOne(new LambdaQueryWrapper<PrivateSessionEntity>()
                .eq(PrivateSessionEntity::getUserAId, userAId)
                .eq(PrivateSessionEntity::getUserBId, userBId)
                .last("limit 1"));
        if (exists != null) {
            return toSessionVO(exists, currentUserId);
        }

        LocalDateTime now = LocalDateTime.now();
        PrivateSessionEntity session = new PrivateSessionEntity();
        session.setUserAId(userAId);
        session.setUserBId(userBId);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);

        try {
            privateSessionMapper.insert(session);
        } catch (DuplicateKeyException e) {
            PrivateSessionEntity concurrent = privateSessionMapper.selectOne(new LambdaQueryWrapper<PrivateSessionEntity>()
                    .eq(PrivateSessionEntity::getUserAId, userAId)
                    .eq(PrivateSessionEntity::getUserBId, userBId)
                    .last("limit 1"));
            if (concurrent != null) {
                return toSessionVO(concurrent, currentUserId);
            }
            throw e;
        }

        return toSessionVO(session, currentUserId);
    }

    public PagedResult<PrivateSessionVO> sessions(long page, long size) {
        Long currentUserId = authService.currentUserId();

        Page<PrivateSessionEntity> data = privateSessionMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<PrivateSessionEntity>()
                        .and(w -> w.eq(PrivateSessionEntity::getUserAId, currentUserId)
                                .or()
                                .eq(PrivateSessionEntity::getUserBId, currentUserId))
                        .orderByDesc(PrivateSessionEntity::getLastMessageAt)
                        .orderByDesc(PrivateSessionEntity::getUpdatedAt)
        );

        List<PrivateSessionVO> records = data.getRecords().stream()
                .map(item -> toSessionVO(item, currentUserId))
                .toList();
        return new PagedResult<>(records, data.getTotal(), data.getCurrent(), data.getSize());
    }

    public PagedResult<PrivateMessageVO> messages(Long sessionId, long page, long size) {
        Long currentUserId = authService.currentUserId();
        PrivateSessionEntity session = getOwnedSession(sessionId, currentUserId);

        Page<PrivateMessageEntity> data = privateMessageMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<PrivateMessageEntity>()
                        .eq(PrivateMessageEntity::getSessionId, session.getId())
                        .orderByDesc(PrivateMessageEntity::getCreatedAt)
        );

        List<PrivateMessageVO> records = data.getRecords().stream()
                .map(this::toMessageVO)
                .toList();
        return new PagedResult<>(records, data.getTotal(), data.getCurrent(), data.getSize());
    }

    public PrivateMessageVO send(Long sessionId, SendPrivateMessageRequest request) {
        Long currentUserId = authService.currentUserId();
        PrivateSessionEntity session = getOwnedSession(sessionId, currentUserId);

        String content = request.getContent().trim();
        if (content.isEmpty()) {
            throw new BusinessException("消息内容必填");
        }

        PrivateMessageEntity message = new PrivateMessageEntity();
        message.setSessionId(session.getId());
        message.setSenderUserId(currentUserId);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        privateMessageMapper.insert(message);

        session.setLastMessageId(message.getId());
        session.setLastMessageAt(message.getCreatedAt());
        session.setUpdatedAt(message.getCreatedAt());
        privateSessionMapper.updateById(session);

        return toMessageVO(message);
    }

    private PrivateSessionEntity getOwnedSession(Long sessionId, Long currentUserId) {
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException("会话不存在");
        }

        PrivateSessionEntity session = privateSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }

        if (!currentUserId.equals(session.getUserAId()) && !currentUserId.equals(session.getUserBId())) {
            throw new BusinessException("无权限访问该会话");
        }
        return session;
    }

    private PrivateSessionVO toSessionVO(PrivateSessionEntity session, Long currentUserId) {
        Long peerUserId = currentUserId.equals(session.getUserAId()) ? session.getUserBId() : session.getUserAId();
        UserEntity peer = userMapper.selectById(peerUserId);

        String lastMessage = null;
        if (session.getLastMessageId() != null) {
            PrivateMessageEntity last = privateMessageMapper.selectById(session.getLastMessageId());
            if (last != null) {
                lastMessage = last.getContent();
            }
        }

        if (peer == null) {
            return PrivateSessionVO.builder()
                    .id(session.getId())
                    .peerUserId(peerUserId)
                    .peerUsername("用户")
                    .peerAvatar(null)
                    .peerBio(null)
                    .lastMessage(lastMessage)
                    .lastMessageAt(session.getLastMessageAt())
                    .build();
        }

        return PrivateSessionVO.builder()
                .id(session.getId())
                .peerUserId(peer.getId())
                .peerUsername(peer.getUsername())
                .peerAvatar(peer.getAvatar())
                .peerBio(peer.getBio())
                .lastMessage(lastMessage)
                .lastMessageAt(session.getLastMessageAt())
                .build();
    }

    private PrivateMessageVO toMessageVO(PrivateMessageEntity message) {
        UserEntity sender = userMapper.selectById(message.getSenderUserId());
        return PrivateMessageVO.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .senderUserId(message.getSenderUserId())
                .senderUsername(sender == null ? "用户" : sender.getUsername())
                .senderAvatar(sender == null ? null : sender.getAvatar())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
