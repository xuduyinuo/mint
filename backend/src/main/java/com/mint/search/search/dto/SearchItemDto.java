package com.mint.search.search.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SearchItemDto {
    private String externalId;
    private String type;
    private String sourceName;
    private String title;
    private String summary;
    private String url;
    private String thumbnailUrl;
    private String tags;
    private Double authorityScore;
    private Double score;
    private LocalDateTime publishedAt;
}
