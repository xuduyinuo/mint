package com.mint.search.source;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("search_source")
public class SearchSource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String type;
    private Integer enabled;
    private Double weight;
    private Double authorityScore;
    private String configJson;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
