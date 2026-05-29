package com.mint.search.ranking;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ranking_config")
public class RankingConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Double relevanceWeight;
    private Double freshnessWeight;
    private Double authorityWeight;
    private Double preferenceWeight;
    private Integer enabled;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
