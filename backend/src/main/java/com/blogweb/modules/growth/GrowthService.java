package com.blogweb.modules.growth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blogweb.common.BusinessException;
import com.blogweb.config.MainUserProperties;
import com.blogweb.modules.moment.MomentCommentEntity;
import com.blogweb.modules.moment.MomentCommentMapper;
import com.blogweb.modules.moment.MomentEntity;
import com.blogweb.modules.moment.MomentLikeEntity;
import com.blogweb.modules.moment.MomentLikeMapper;
import com.blogweb.modules.moment.MomentMapper;
import com.blogweb.modules.note.NoteCommentEntity;
import com.blogweb.modules.note.NoteCommentMapper;
import com.blogweb.modules.note.NoteEntity;
import com.blogweb.modules.note.NoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GrowthService {

    private final NoteMapper noteMapper;
    private final MomentMapper momentMapper;
    private final NoteCommentMapper noteCommentMapper;
    private final MomentCommentMapper momentCommentMapper;
    private final MomentLikeMapper momentLikeMapper;
    private final MainUserProperties mainUserProperties;

    public GrowthOverviewVO overview(int days) {
        Long mainUserId = mainUserId();
        int safeDays = normalizeDays(days);
        LocalDate startDate = LocalDate.now().minusDays(safeDays);
        LocalDateTime startDateTime = startDate.atStartOfDay();

        List<NoteEntity> notes = noteMapper.selectList(new LambdaQueryWrapper<NoteEntity>()
                .eq(NoteEntity::getUserId, mainUserId)
                .orderByAsc(NoteEntity::getUpdatedAt)
                .orderByAsc(NoteEntity::getCreatedAt));

        List<MomentEntity> moments = momentMapper.selectList(new LambdaQueryWrapper<MomentEntity>()
                .eq(MomentEntity::getUserId, mainUserId)
                .orderByAsc(MomentEntity::getCreatedAt));

        Map<String, Long> timelineMap = new HashMap<>();
        Map<String, Long> tagMap = new HashMap<>();
        Map<Integer, Long> weekdayMap = new HashMap<>();
        Map<String, Long> statusMap = new HashMap<>();
        Map<String, long[]> loopTimelineMap = new HashMap<>();

        long notesCreated = 0L;
        long momentsCreated = 0L;
        long notesIterated = 0L;
        double iterationLagDaysSum = 0.0;

        for (NoteEntity note : notes) {
            LocalDateTime statTime = statTime(note);
            if (statTime == null || statTime.toLocalDate().isBefore(startDate)) {
                continue;
            }

            String date = statTime.toLocalDate().format(DateTimeFormatter.ISO_DATE);
            timelineMap.put(date, timelineMap.getOrDefault(date, 0L) + 1);

            int weekday = dayValue(statTime.getDayOfWeek());
            weekdayMap.put(weekday, weekdayMap.getOrDefault(weekday, 0L) + 1);

            if (StringUtils.hasText(note.getStatus())) {
                String key = note.getStatus().trim();
                statusMap.put(key, statusMap.getOrDefault(key, 0L) + 1);
            }

            if (StringUtils.hasText(note.getTags())) {
                Arrays.stream(note.getTags().split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .forEach(tag -> tagMap.put(tag, tagMap.getOrDefault(tag, 0L) + 1));
            }
        }

        for (NoteEntity note : notes) {
            if (inWindow(note.getCreatedAt(), startDate)) {
                notesCreated++;
                increaseLoopCount(loopTimelineMap, note.getCreatedAt().toLocalDate(), 0);
            }

            if (note.getCreatedAt() != null
                    && note.getUpdatedAt() != null
                    && note.getUpdatedAt().isAfter(note.getCreatedAt())
                    && inWindow(note.getUpdatedAt(), startDate)) {
                notesIterated++;
                iterationLagDaysSum += Duration.between(note.getCreatedAt(), note.getUpdatedAt()).toHours() / 24.0;
                increaseLoopCount(loopTimelineMap, note.getUpdatedAt().toLocalDate(), 2);
            }
        }

        for (MomentEntity moment : moments) {
            if (inWindow(moment.getCreatedAt(), startDate)) {
                momentsCreated++;
                increaseLoopCount(loopTimelineMap, moment.getCreatedAt().toLocalDate(), 0);
            }
        }

        Set<Long> noteIds = notes.stream().map(NoteEntity::getId).collect(Collectors.toSet());
        Set<Long> momentIds = moments.stream().map(MomentEntity::getId).collect(Collectors.toSet());

        List<NoteCommentEntity> noteComments = noteIds.isEmpty()
                ? List.of()
                : noteCommentMapper.selectList(new LambdaQueryWrapper<NoteCommentEntity>()
                .in(NoteCommentEntity::getNoteId, noteIds)
                .ge(NoteCommentEntity::getCreatedAt, startDateTime));

        List<MomentCommentEntity> momentComments = momentIds.isEmpty()
                ? List.of()
                : momentCommentMapper.selectList(new LambdaQueryWrapper<MomentCommentEntity>()
                .in(MomentCommentEntity::getMomentId, momentIds)
                .ge(MomentCommentEntity::getCreatedAt, startDateTime));

        List<MomentLikeEntity> momentLikes = momentIds.isEmpty()
                ? List.of()
                : momentLikeMapper.selectList(new LambdaQueryWrapper<MomentLikeEntity>()
                .in(MomentLikeEntity::getMomentId, momentIds)
                .ge(MomentLikeEntity::getCreatedAt, startDateTime));

        long noteCommentsReceived = 0L;
        for (NoteCommentEntity comment : noteComments) {
            if (inWindow(comment.getCreatedAt(), startDate)) {
                noteCommentsReceived++;
                increaseLoopCount(loopTimelineMap, comment.getCreatedAt().toLocalDate(), 1);
            }
        }

        long momentCommentsReceived = 0L;
        for (MomentCommentEntity comment : momentComments) {
            if (inWindow(comment.getCreatedAt(), startDate)) {
                momentCommentsReceived++;
                increaseLoopCount(loopTimelineMap, comment.getCreatedAt().toLocalDate(), 1);
            }
        }

        long momentLikesReceived = 0L;
        for (MomentLikeEntity like : momentLikes) {
            if (inWindow(like.getCreatedAt(), startDate)) {
                momentLikesReceived++;
                increaseLoopCount(loopTimelineMap, like.getCreatedAt().toLocalDate(), 1);
            }
        }

        long totalOutput = notesCreated + momentsCreated;
        long totalFeedback = noteCommentsReceived + momentCommentsReceived + momentLikesReceived;
        double iterationCoverage = notesCreated == 0 ? 0.0 : (double) notesIterated / notesCreated;
        double avgIterationLagDays = notesIterated == 0 ? 0.0 : iterationLagDaysSum / notesIterated;
        double feedbackPerOutput = totalOutput == 0 ? 0.0 : (double) totalFeedback / totalOutput;

        LoopSummaryVO loopSummary = new LoopSummaryVO(
                notesCreated,
                momentsCreated,
                totalOutput,
                noteCommentsReceived,
                momentCommentsReceived,
                momentLikesReceived,
                totalFeedback,
                notesIterated,
                iterationCoverage,
                avgIterationLagDays,
                feedbackPerOutput
        );

        List<LoopTimelinePointVO> loopTimeline = loopTimelineMap.entrySet().stream()
                .map(entry -> new LoopTimelinePointVO(
                        entry.getKey(),
                        entry.getValue()[0],
                        entry.getValue()[1],
                        entry.getValue()[2]
                ))
                .sorted(Comparator.comparing(LoopTimelinePointVO::getDate))
                .toList();

        List<TimelinePoint> timeline = new ArrayList<>(timelineMap.entrySet().stream()
                .map(entry -> new TimelinePoint(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(TimelinePoint::getDate))
                .toList());

        List<SkillPoint> skills = tagMap.entrySet().stream()
                .map(entry -> new SkillPoint(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(8)
                .toList();

        List<WeekdayPoint> weekday = weekdayPoints(weekdayMap);
        List<StatusPoint> statuses = statusPoints(statusMap);
        GrowthSummaryVO summary = summaryOf(timeline, safeDays);

        return new GrowthOverviewVO(timeline, skills, weekday, statuses, summary, loopSummary, loopTimeline);
    }

    private void increaseLoopCount(Map<String, long[]> loopTimelineMap, LocalDate date, int bucket) {
        String key = date.format(DateTimeFormatter.ISO_DATE);
        long[] values = loopTimelineMap.computeIfAbsent(key, ignored -> new long[]{0L, 0L, 0L});
        values[bucket] = values[bucket] + 1;
    }

    private boolean inWindow(LocalDateTime dateTime, LocalDate startDate) {
        return dateTime != null && !dateTime.toLocalDate().isBefore(startDate);
    }

    private int dayValue(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> 1;
            case TUESDAY -> 2;
            case WEDNESDAY -> 3;
            case THURSDAY -> 4;
            case FRIDAY -> 5;
            case SATURDAY -> 6;
            case SUNDAY -> 7;
        };
    }

    private List<WeekdayPoint> weekdayPoints(Map<Integer, Long> weekdayMap) {
        Map<Integer, String> labels = new LinkedHashMap<>();
        labels.put(1, "周一");
        labels.put(2, "周二");
        labels.put(3, "周三");
        labels.put(4, "周四");
        labels.put(5, "周五");
        labels.put(6, "周六");
        labels.put(7, "周日");

        List<WeekdayPoint> points = new ArrayList<>();
        labels.forEach((key, label) -> points.add(new WeekdayPoint(label, weekdayMap.getOrDefault(key, 0L))));
        return points;
    }

    private List<StatusPoint> statusPoints(Map<String, Long> statusMap) {
        List<StatusPoint> points = new ArrayList<>();
        points.add(new StatusPoint("LEARNING", statusMap.getOrDefault("LEARNING", 0L)));
        points.add(new StatusPoint("MASTERED", statusMap.getOrDefault("MASTERED", 0L)));
        return points;
    }

    private GrowthSummaryVO summaryOf(List<TimelinePoint> timeline, int days) {
        long totalNotes = timeline.stream().mapToLong(TimelinePoint::getCount).sum();
        long activeDays = timeline.size();
        long peakDayCount = timeline.stream().mapToLong(TimelinePoint::getCount).max().orElse(0L);
        long avgPerWeek = Math.round(totalNotes * 7.0 / days);
        return new GrowthSummaryVO(totalNotes, activeDays, peakDayCount, avgPerWeek);
    }

    private LocalDateTime statTime(NoteEntity note) {
        if (note.getUpdatedAt() != null) {
            return note.getUpdatedAt();
        }
        return note.getCreatedAt();
    }

    private int normalizeDays(int days) {
        if (days < 1) {
            return 1;
        }
        return Math.min(days, 3650);
    }

    private Long mainUserId() {
        Long id = mainUserProperties.getId();
        if (id == null || id <= 0) {
            throw new BusinessException("主用户配置无效");
        }
        return id;
    }
}
