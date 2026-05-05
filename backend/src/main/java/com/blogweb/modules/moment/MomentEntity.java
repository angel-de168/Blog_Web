package com.blogweb.modules.moment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("moment")
public class MomentEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String content;
    private String mood;
    private String visibilityScope;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
