package com.blogweb.modules.site;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blogweb.common.BusinessException;
import com.blogweb.config.MainUserProperties;
import com.blogweb.modules.note.NoteEntity;
import com.blogweb.modules.note.NoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SiteService {

    private static final String HOME_VISIT_COUNTER = "home_visit";

    private final NoteMapper noteMapper;
    private final SiteCounterMapper siteCounterMapper;
    private final MainUserProperties mainUserProperties;

    public SiteStatsVO stats() {
        Long mainUserId = mainUserId();

        LambdaQueryWrapper<NoteEntity> wrapper = new LambdaQueryWrapper<NoteEntity>()
                .eq(NoteEntity::getUserId, mainUserId)
                .select(NoteEntity::getId, NoteEntity::getTags);

        var notes = noteMapper.selectList(wrapper);

        Set<String> tagSet = new HashSet<>();
        for (NoteEntity note : notes) {
            if (!StringUtils.hasText(note.getTags())) {
                continue;
            }
            Arrays.stream(note.getTags().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(tagSet::add);
        }

        SiteCounterEntity counter = siteCounterMapper.selectById(HOME_VISIT_COUNTER);
        if (counter == null) {
            counter = new SiteCounterEntity();
            counter.setName(HOME_VISIT_COUNTER);
            counter.setValue(1L);
            counter.setUpdatedAt(LocalDateTime.now());
            siteCounterMapper.insert(counter);
        } else {
            counter.setValue(counter.getValue() + 1);
            counter.setUpdatedAt(LocalDateTime.now());
            siteCounterMapper.updateById(counter);
        }

        return new SiteStatsVO((long) notes.size(), (long) tagSet.size(), counter.getValue());
    }

    private Long mainUserId() {
        Long id = mainUserProperties.getId();
        if (id == null || id <= 0) {
            throw new BusinessException("主用户配置无效");
        }
        return id;
    }
}
