package com.mint.search.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class SearchResponse {
    private String keyword;
    private String type;
    private List<SearchItemDto> records;
    private long total;
    private int page;
    private int size;
    private String rankingExplain;
    private Map<String, Long> sourceDistribution;
}
