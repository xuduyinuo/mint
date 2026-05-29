package com.mint.search.search;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("search_item")
public class SearchItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String externalId;
    private String type;
    private String sourceName;
    private String title;
    private String summary;
    private String url;
    private String thumbnailUrl;
    private String tags;
    private Double authorityScore;
    private LocalDateTime publishedAt;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
