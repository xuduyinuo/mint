package com.mint.search.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminBlogContentDto {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String title;
    private String summary;
    private String content;
    private String status;
    private Integer blocked;
    private String tags;
    private String coverUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
