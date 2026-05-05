package com.blogweb.modules.note;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteMapper noteMapper;
    private final NoteCommentMapper noteCommentMapper;
    private final UserMapper userMapper;
    private final AuthService authService;
    private final MainUserProperties mainUserProperties;

    public PagedResult<NoteVO> list(String keyword,
                                     String status,
                                     String contentKeyword,
                                     String startTime,
                                     String endTime,
                                     Long minReadCount,
                                     Long maxReadCount,
                                     String sortBy,
                                     String sortOrder,
                                     long page,
                                     long size) {
        Long mainUserId = mainUserId();

        LambdaQueryWrapper<NoteEntity> wrapper = new LambdaQueryWrapper<NoteEntity>()
                .eq(NoteEntity::getUserId, mainUserId);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(NoteEntity::getTitle, keyword.trim()).or().like(NoteEntity::getContent, keyword.trim()));
        }
        if (StringUtils.hasText(contentKeyword)) {
            wrapper.like(NoteEntity::getContent, contentKeyword.trim());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(NoteEntity::getStatus, status.trim());
        }

        LocalDateTime startAt = parseStartTime(startTime);
        LocalDateTime endAt = parseEndTime(endTime);
        if (startAt != null) {
            wrapper.ge(NoteEntity::getUpdatedAt, startAt);
        }
        if (endAt != null) {
            wrapper.le(NoteEntity::getUpdatedAt, endAt);
        }

        if (minReadCount != null) {
            wrapper.ge(NoteEntity::getReadCount, minReadCount);
        }
        if (maxReadCount != null) {
            wrapper.le(NoteEntity::getReadCount, maxReadCount);
        }

        if (minReadCount != null && maxReadCount != null && minReadCount > maxReadCount) {
            throw new BusinessException("阅读量范围无效");
        }

        applySort(wrapper, sortBy, sortOrder);

        Page<NoteEntity> data = noteMapper.selectPage(new Page<>(page, size), wrapper);
        List<NoteVO> records = data.getRecords().stream().map(this::toVO).toList();
        return new PagedResult<>(records, data.getTotal(), page, size);
    }

    public NoteVO detail(Long id) {
        NoteEntity note = getMainUserNote(id);
        long currentReadCount = note.getReadCount() == null ? 0L : note.getReadCount();
        note.setReadCount(currentReadCount + 1);
        noteMapper.updateById(note);
        return toVO(note);
    }

    public PagedResult<NoteCommentVO> listComments(Long noteId, long page, long size) {
        getMainUserNote(noteId);

        Page<NoteCommentEntity> data = noteCommentMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<NoteCommentEntity>()
                        .eq(NoteCommentEntity::getNoteId, noteId)
                        .orderByDesc(NoteCommentEntity::getCreatedAt)
        );

        List<NoteCommentVO> records = data.getRecords().stream().map(this::toCommentVO).toList();
        return new PagedResult<>(records, data.getTotal(), data.getCurrent(), data.getSize());
    }

    public NoteCommentVO addComment(Long noteId, SaveNoteCommentRequest request) {
        Long userId = authService.currentUserId();
        getMainUserNote(noteId);

        NoteCommentEntity entity = new NoteCommentEntity();
        entity.setNoteId(noteId);
        entity.setUserId(userId);
        entity.setContent(request.getContent().trim());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        noteCommentMapper.insert(entity);

        return toCommentVO(entity);
    }

    public void removeComment(Long noteId, Long commentId) {
        Long userId = authService.currentUserId();
        getMainUserNote(noteId);

        NoteCommentEntity comment = noteCommentMapper.selectById(commentId);
        if (comment == null || !comment.getNoteId().equals(noteId) || !comment.getUserId().equals(userId)) {
            throw new BusinessException("评论不存在");
        }
        noteCommentMapper.deleteById(commentId);
    }

    public NoteVO save(SaveNoteRequest request) {
        Long userId = authService.currentUserId();
        Long mainUserId = mainUserId();
        if (!userId.equals(mainUserId)) {
            throw new BusinessException("仅主用户可发布博客");
        }
        NoteEntity note;

        if (request.getId() == null) {
            note = new NoteEntity();
            note.setUserId(userId);
            note.setReadCount(0L);
            note.setCreatedAt(LocalDateTime.now());
        } else {
            note = noteMapper.selectById(request.getId());
            if (note == null || !note.getUserId().equals(userId)) {
                throw new BusinessException("笔记不存在");
            }
        }

        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setCategoryId(request.getCategoryId());
        note.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "LEARNING");
        note.setTags(request.getTags() == null ? "" : String.join(",", request.getTags()));
        if (StringUtils.hasText(request.getCoverImage())) {
            note.setCoverImage(request.getCoverImage().trim());
        }
        note.setUpdatedAt(LocalDateTime.now());

        if (request.getId() == null) {
            noteMapper.insert(note);
        } else {
            noteMapper.updateById(note);
        }

        return toVO(note);
    }

    public void remove(Long id) {
        Long userId = authService.currentUserId();
        Long mainUserId = mainUserId();
        if (!userId.equals(mainUserId)) {
            throw new BusinessException("仅主用户可删除博客");
        }
        NoteEntity note = noteMapper.selectById(id);
        if (note == null || !note.getUserId().equals(userId)) {
            throw new BusinessException("笔记不存在");
        }
        noteCommentMapper.delete(new LambdaQueryWrapper<NoteCommentEntity>().eq(NoteCommentEntity::getNoteId, id));
        noteMapper.deleteById(id);
    }

    private void applySort(LambdaQueryWrapper<NoteEntity> wrapper, String sortBy, String sortOrder) {
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        if ("readCount".equals(sortBy)) {
            wrapper.orderBy(true, asc, NoteEntity::getReadCount).orderByDesc(NoteEntity::getUpdatedAt);
            return;
        }
        if ("contentLength".equals(sortBy)) {
            wrapper.last("ORDER BY CHAR_LENGTH(content) " + (asc ? "ASC" : "DESC") + ", updated_at DESC");
            return;
        }
        wrapper.orderBy(true, asc, NoteEntity::getUpdatedAt);
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

    private NoteEntity getMainUserNote(Long noteId) {
        NoteEntity note = noteMapper.selectById(noteId);
        if (note == null || !note.getUserId().equals(mainUserId())) {
            throw new BusinessException("笔记不存在");
        }
        return note;
    }

    private NoteVO toVO(NoteEntity note) {
        List<String> tags = StringUtils.hasText(note.getTags())
                ? Arrays.stream(note.getTags().split(",")).map(String::trim).filter(StringUtils::hasText).toList()
                : Collections.emptyList();

        Long commentCount = noteCommentMapper.selectCount(
                new LambdaQueryWrapper<NoteCommentEntity>().eq(NoteCommentEntity::getNoteId, note.getId())
        );
        UserEntity author = userMapper.selectById(note.getUserId());

        return NoteVO.builder()
                .id(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .categoryId(note.getCategoryId())
                .categoryName(null)
                .status(note.getStatus())
                .tags(tags)
                .coverImage(note.getCoverImage())
                .readCount(note.getReadCount() == null ? 0L : note.getReadCount())
                .commentCount(commentCount == null ? 0L : commentCount)
                .authorName(author == null ? "作者" : author.getUsername())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }

    private NoteCommentVO toCommentVO(NoteCommentEntity comment) {
        UserEntity user = userMapper.selectById(comment.getUserId());
        return NoteCommentVO.builder()
                .id(comment.getId())
                .userId(comment.getUserId())
                .username(user == null ? "用户" : user.getUsername())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
