package com.blogweb.modules.moment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blogweb.common.BusinessException;
import com.blogweb.common.PagedResult;
import com.blogweb.config.MainUserProperties;
import com.blogweb.modules.auth.AuthService;
import com.blogweb.modules.user.UserEntity;
import com.blogweb.modules.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MomentService {

    private static final String SCOPE_PUBLIC = "PUBLIC";
    private static final String SCOPE_PRIVATE_SELF = "PRIVATE_SELF";
    private static final String SCOPE_CIRCLE = "CIRCLE";

    private final MomentMapper momentMapper;
    private final MomentLikeMapper momentLikeMapper;
    private final MomentCommentMapper momentCommentMapper;
    private final UserMapper userMapper;
    private final AuthService authService;
    private final MainUserProperties mainUserProperties;

    public PagedResult<MomentVO> list(String keyword,
                                      String mood,
                                      String contentKeyword,
                                      String visibilityScope,
                                      String startTime,
                                      String endTime,
                                      Long minLikeCount,
                                      Long maxLikeCount,
                                      String sortBy,
                                      String sortOrder,
                                      long page,
                                      long size) {
        Long mainUserId = mainUserId();
        Long viewerUserId = authService.currentUserIdOptional().orElse(null);

        LambdaQueryWrapper<MomentEntity> wrapper = new LambdaQueryWrapper<MomentEntity>()
                .eq(MomentEntity::getUserId, mainUserId);

        applyVisibilityListScope(wrapper, viewerUserId, visibilityScope);

        if (StringUtils.hasText(keyword)) {
            String trimmed = keyword.trim();
            wrapper.and(w -> w.like(MomentEntity::getContent, trimmed).or().like(MomentEntity::getMood, trimmed));
        }
        if (StringUtils.hasText(mood)) {
            wrapper.like(MomentEntity::getMood, mood.trim());
        }
        if (StringUtils.hasText(contentKeyword)) {
            wrapper.like(MomentEntity::getContent, contentKeyword.trim());
        }

        LocalDateTime startAt = parseStartTime(startTime);
        LocalDateTime endAt = parseEndTime(endTime);
        if (startAt != null) {
            wrapper.ge(MomentEntity::getUpdatedAt, startAt);
        }
        if (endAt != null) {
            wrapper.le(MomentEntity::getUpdatedAt, endAt);
        }

        if (minLikeCount != null) {
            wrapper.apply("(select count(1) from moment_like ml where ml.moment_id = id) >= {0}", minLikeCount);
        }
        if (maxLikeCount != null) {
            wrapper.apply("(select count(1) from moment_like ml where ml.moment_id = id) <= {0}", maxLikeCount);
        }
        if (minLikeCount != null && maxLikeCount != null && minLikeCount > maxLikeCount) {
            throw new BusinessException("点赞量范围无效");
        }

        applySort(wrapper, sortBy, sortOrder);

        Page<MomentEntity> data = momentMapper.selectPage(new Page<>(page, size), wrapper);
        List<MomentVO> records = data.getRecords().stream().map(item -> toVO(item, viewerUserId)).toList();
        return new PagedResult<>(records, data.getTotal(), data.getCurrent(), data.getSize());
    }

    public MomentVO detail(Long id) {
        Long viewerUserId = authService.currentUserIdOptional().orElse(null);
        MomentEntity moment = getAccessibleMainUserMoment(id, viewerUserId);
        return toVO(moment, viewerUserId);
    }

    public MomentVO save(SaveMomentRequest request) {
        Long userId = authService.currentUserId();
        Long mainUserId = mainUserId();
        if (!userId.equals(mainUserId)) {
            throw new BusinessException("仅主用户可发布随笔");
        }
        MomentEntity moment;

        if (request.getId() == null) {
            moment = new MomentEntity();
            moment.setUserId(userId);
            moment.setCreatedAt(LocalDateTime.now());
        } else {
            moment = getOwnedMoment(request.getId(), userId);
        }

        moment.setContent(request.getContent());
        moment.setMood(StringUtils.hasText(request.getMood()) ? request.getMood() : "");
        moment.setVisibilityScope(normalizeScopeForSave(request.getVisibilityScope()));
        moment.setUpdatedAt(LocalDateTime.now());

        if (request.getId() == null) {
            momentMapper.insert(moment);
        } else {
            momentMapper.updateById(moment);
        }

        return toVO(moment, userId);
    }

    public void remove(Long id) {
        Long userId = authService.currentUserId();
        Long mainUserId = mainUserId();
        if (!userId.equals(mainUserId)) {
            throw new BusinessException("仅主用户可删除随笔");
        }
        getOwnedMoment(id, userId);

        momentMapper.deleteById(id);
        momentLikeMapper.delete(new LambdaQueryWrapper<MomentLikeEntity>().eq(MomentLikeEntity::getMomentId, id));
        momentCommentMapper.delete(new LambdaQueryWrapper<MomentCommentEntity>().eq(MomentCommentEntity::getMomentId, id));
    }

    public MomentVO like(Long momentId) {
        Long userId = authService.currentUserId();
        getAccessibleMainUserMoment(momentId, userId);

        MomentLikeEntity like = momentLikeMapper.selectOne(new LambdaQueryWrapper<MomentLikeEntity>()
                .eq(MomentLikeEntity::getMomentId, momentId)
                .eq(MomentLikeEntity::getUserId, userId)
                .last("limit 1"));

        if (like == null) {
            MomentLikeEntity entity = new MomentLikeEntity();
            entity.setMomentId(momentId);
            entity.setUserId(userId);
            entity.setCreatedAt(LocalDateTime.now());
            momentLikeMapper.insert(entity);
        }

        MomentEntity moment = momentMapper.selectById(momentId);
        return toVO(moment, userId);
    }

    public MomentVO unlike(Long momentId) {
        Long userId = authService.currentUserId();
        getAccessibleMainUserMoment(momentId, userId);

        momentLikeMapper.delete(new LambdaQueryWrapper<MomentLikeEntity>()
                .eq(MomentLikeEntity::getMomentId, momentId)
                .eq(MomentLikeEntity::getUserId, userId));

        MomentEntity moment = momentMapper.selectById(momentId);
        return toVO(moment, userId);
    }

    public PagedResult<MomentCommentVO> listComments(Long momentId, long page, long size) {
        Long viewerUserId = authService.currentUserIdOptional().orElse(null);
        getAccessibleMainUserMoment(momentId, viewerUserId);

        Page<MomentCommentEntity> data = momentCommentMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<MomentCommentEntity>()
                        .eq(MomentCommentEntity::getMomentId, momentId)
                        .orderByDesc(MomentCommentEntity::getCreatedAt)
        );

        List<MomentCommentVO> records = data.getRecords().stream().map(this::toCommentVO).toList();
        return new PagedResult<>(records, data.getTotal(), data.getCurrent(), data.getSize());
    }

    public MomentCommentVO addComment(Long momentId, SaveMomentCommentRequest request) {
        Long userId = authService.currentUserId();
        getAccessibleMainUserMoment(momentId, userId);

        MomentCommentEntity entity = new MomentCommentEntity();
        entity.setMomentId(momentId);
        entity.setUserId(userId);
        entity.setContent(request.getContent().trim());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        momentCommentMapper.insert(entity);

        return toCommentVO(entity);
    }

    public void removeComment(Long momentId, Long commentId) {
        Long userId = authService.currentUserId();
        getAccessibleMainUserMoment(momentId, userId);

        MomentCommentEntity comment = momentCommentMapper.selectById(commentId);
        if (comment == null || !comment.getMomentId().equals(momentId) || !comment.getUserId().equals(userId)) {
            throw new BusinessException("评论不存在");
        }
        momentCommentMapper.deleteById(commentId);
    }

    private void applyVisibilityListScope(LambdaQueryWrapper<MomentEntity> wrapper, Long viewerUserId, String filterVisibilityScope) {
        String scopeFilter = null;
        if (StringUtils.hasText(filterVisibilityScope)) {
            scopeFilter = normalizeScopeForFilter(filterVisibilityScope);
        }

        if (isMainUserViewer(viewerUserId)) {
            wrapper.in(MomentEntity::getVisibilityScope, SCOPE_PUBLIC, SCOPE_PRIVATE_SELF, SCOPE_CIRCLE);
            if (scopeFilter != null) {
                wrapper.eq(MomentEntity::getVisibilityScope, scopeFilter);
            }
            return;
        }

        if (SCOPE_PRIVATE_SELF.equals(scopeFilter) || SCOPE_CIRCLE.equals(scopeFilter)) {
            wrapper.apply("1 = 0");
            return;
        }

        wrapper.eq(MomentEntity::getVisibilityScope, SCOPE_PUBLIC);
    }



    private void applySort(LambdaQueryWrapper<MomentEntity> wrapper, String sortBy, String sortOrder) {
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        if ("likeCount".equals(sortBy)) {
            wrapper.last("ORDER BY (SELECT COUNT(1) FROM moment_like ml WHERE ml.moment_id = id) " + (asc ? "ASC" : "DESC") + ", updated_at DESC");
            return;
        }
        if ("commentCount".equals(sortBy)) {
            wrapper.last("ORDER BY (SELECT COUNT(1) FROM moment_comment mc WHERE mc.moment_id = id) " + (asc ? "ASC" : "DESC") + ", updated_at DESC");
            return;
        }
        if ("contentLength".equals(sortBy)) {
            wrapper.last("ORDER BY CHAR_LENGTH(content) " + (asc ? "ASC" : "DESC") + ", updated_at DESC");
            return;
        }
        wrapper.orderBy(true, asc, MomentEntity::getUpdatedAt);
    }

    private LocalDateTime parseStartTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ignored) {
            return LocalDate.parse(value.trim()).atStartOfDay();
        }
    }

    private LocalDateTime parseEndTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ignored) {
            return LocalDate.parse(value.trim()).atTime(23, 59, 59);
        }
    }

    private Long mainUserId() {
        Long id = mainUserProperties.getId();
        if (id == null || id <= 0) {
            throw new BusinessException("主用户配置无效");
        }
        return id;
    }

    private MomentEntity getAccessibleMainUserMoment(Long momentId, Long viewerUserId) {
        MomentEntity moment = momentMapper.selectById(momentId);
        if (moment == null || !moment.getUserId().equals(mainUserId()) || !canView(moment, viewerUserId)) {
            throw new BusinessException("随笔不存在");
        }
        return moment;
    }

    private MomentEntity getOwnedMoment(Long momentId, Long userId) {
        MomentEntity moment = momentMapper.selectById(momentId);
        if (moment == null || !moment.getUserId().equals(userId)) {
            throw new BusinessException("随笔不存在");
        }
        return moment;
    }

    private boolean canView(MomentEntity moment, Long viewerUserId) {
        String scope = normalizeScopeFromStorage(moment.getVisibilityScope());
        return switch (scope) {
            case SCOPE_PUBLIC -> true;
            case SCOPE_PRIVATE_SELF, SCOPE_CIRCLE -> isMainUserViewer(viewerUserId);
            default -> false;
        };
    }

    private boolean isMainUserViewer(Long viewerUserId) {
        return viewerUserId != null && viewerUserId.equals(mainUserId());
    }

    private String normalizeScopeForSave(String rawScope) {
        if (!StringUtils.hasText(rawScope)) {
            return SCOPE_PUBLIC;
        }
        String normalized = normalizeScopeToken(rawScope);
        if (!SCOPE_PUBLIC.equals(normalized) && !SCOPE_PRIVATE_SELF.equals(normalized) && !SCOPE_CIRCLE.equals(normalized)) {
            throw new BusinessException("可见范围无效");
        }
        return normalized;
    }

    private String normalizeScopeForFilter(String rawScope) {
        String normalized = normalizeScopeToken(rawScope);
        if (!SCOPE_PUBLIC.equals(normalized) && !SCOPE_PRIVATE_SELF.equals(normalized) && !SCOPE_CIRCLE.equals(normalized)) {
            throw new BusinessException("可见范围无效");
        }
        return normalized;
    }

    private String normalizeScopeFromStorage(String rawScope) {
        if (!StringUtils.hasText(rawScope)) {
            return SCOPE_PUBLIC;
        }
        String normalized = normalizeScopeToken(rawScope);
        if (SCOPE_PUBLIC.equals(normalized) || SCOPE_PRIVATE_SELF.equals(normalized) || SCOPE_CIRCLE.equals(normalized)) {
            return normalized;
        }
        return SCOPE_PUBLIC;
    }

    private String normalizeScopeToken(String rawScope) {
        return rawScope.trim().toUpperCase(Locale.ROOT);
    }

    private MomentVO toVO(MomentEntity moment, Long userId) {
        Long likeCount = momentLikeMapper.selectCount(new LambdaQueryWrapper<MomentLikeEntity>()
                .eq(MomentLikeEntity::getMomentId, moment.getId()));
        Long commentCount = momentCommentMapper.selectCount(new LambdaQueryWrapper<MomentCommentEntity>()
                .eq(MomentCommentEntity::getMomentId, moment.getId()));
        boolean likedByCurrentUser = false;
        if (userId != null) {
            Long likedCount = momentLikeMapper.selectCount(new LambdaQueryWrapper<MomentLikeEntity>()
                    .eq(MomentLikeEntity::getMomentId, moment.getId())
                    .eq(MomentLikeEntity::getUserId, userId));
            likedByCurrentUser = likedCount != null && likedCount > 0;
        }

        return MomentVO.builder()
                .id(moment.getId())
                .content(moment.getContent())
                .mood(moment.getMood())
                .visibilityScope(normalizeScopeFromStorage(moment.getVisibilityScope()))
                .likeCount(likeCount == null ? 0L : likeCount)
                .commentCount(commentCount == null ? 0L : commentCount)
                .likedByCurrentUser(likedByCurrentUser)
                .createdAt(moment.getCreatedAt())
                .updatedAt(moment.getUpdatedAt())
                .build();
    }

    private MomentCommentVO toCommentVO(MomentCommentEntity comment) {
        UserEntity user = userMapper.selectById(comment.getUserId());
        return MomentCommentVO.builder()
                .id(comment.getId())
                .userId(comment.getUserId())
                .username(user == null ? "用户" : user.getUsername())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
