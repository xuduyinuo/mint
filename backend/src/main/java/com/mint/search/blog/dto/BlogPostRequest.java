package com.mint.search.blog.dto;

import lombok.Data;

@Data
public class BlogPostRequest {
    private String title;
    private String summary;
    private String coverUrl;
    private String tags;
    private String content;
    private String status;
}
