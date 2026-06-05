package com.mint.search.content.analysis;

import java.util.List;

public record ContentAnalysisResult(String summary, List<String> tags, String recommendReason, boolean enhanced) {
    public static ContentAnalysisResult fallback(String summary, String tags, String recommendReason) {
        List<String> tagList = tags == null || tags.isBlank()
                ? List.of()
                : List.of(tags.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .limit(8)
                .toList();
        return new ContentAnalysisResult(summary, tagList, recommendReason, false);
    }
}
