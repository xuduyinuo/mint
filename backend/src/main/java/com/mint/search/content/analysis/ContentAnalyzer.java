package com.mint.search.content.analysis;

import com.mint.search.search.dto.SearchItemDto;

import java.util.Optional;

@FunctionalInterface
public interface ContentAnalyzer {
    Optional<ContentAnalysisResult> analyze(SearchItemDto item);
}
