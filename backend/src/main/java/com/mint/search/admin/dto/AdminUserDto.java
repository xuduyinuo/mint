package com.mint.search.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserDto {
    private Long id;
    private String username;
    private String nickname;
    private String role;
    private Integer status;
    private String interestTags;
    private String preferredTypes;
    private Long behaviorCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
