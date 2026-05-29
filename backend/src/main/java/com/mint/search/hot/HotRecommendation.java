package com.mint.search.hot;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("hot_recommendation")
public class HotRecommendation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String type;
    private String title;
    private String summary;
    private String url;
    private String thumbnailUrl;
    private Double heatScore;
    private Integer enabled;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
