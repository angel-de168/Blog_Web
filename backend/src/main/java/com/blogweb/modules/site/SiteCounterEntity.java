package com.blogweb.modules.site;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("site_counter")
public class SiteCounterEntity {

    @TableId
    private String name;
    private Long value;
    private LocalDateTime updatedAt;
}
