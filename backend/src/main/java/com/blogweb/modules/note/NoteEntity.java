package com.blogweb.modules.note;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("note")
public class NoteEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private Long categoryId;
    private String status;
    private String tags;
    private String coverImage;
    private Long readCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
