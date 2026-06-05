package com.mint.search.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminImageContentDto {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String fileName;
    private String url;
    private String contentType;
    private Long fileSize;
    private String tags;
    private Integer blocked;
    private LocalDateTime createTime;
}
