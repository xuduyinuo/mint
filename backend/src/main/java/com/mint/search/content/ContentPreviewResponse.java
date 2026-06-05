package com.mint.search.content;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ContentPreviewResponse {
    private String title;
    private String type;
    private String sourceName;
    private String originalUrl;
    private String thumbnailUrl;
    private String content;
    private String analysisSummary;
    private List<String> analysisTags;
    private String recommendReason;
    private boolean llmEnhanced;
}
