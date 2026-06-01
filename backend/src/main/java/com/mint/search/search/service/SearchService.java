package com.mint.search.search.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mint.search.log.SearchLog;
import com.mint.search.log.mapper.SearchLogMapper;
import com.mint.search.profile.UserProfile;
import com.mint.search.profile.mapper.UserProfileMapper;
import com.mint.search.search.dto.SearchItemDto;
import com.mint.search.search.dto.SearchResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {
    private static final List<String> SUPPORTED_TYPES = List.of("all", "news", "image", "video");

    private final DomesticSearchProvider domesticSearchProvider;
    private final RankingService rankingService;
    private final SearchLogMapper searchLogMapper;
    private final UserProfileMapper profileMapper;

    public SearchService(DomesticSearchProvider domesticSearchProvider, RankingService rankingService,
                         SearchLogMapper searchLogMapper, UserProfileMapper profileMapper) {
        this.domesticSearchProvider = domesticSearchProvider;
        this.rankingService = rankingService;
        this.searchLogMapper = searchLogMapper;
        this.profileMapper = profileMapper;
    }

    public SearchResponse search(String keyword, String type, int page, int size, Long userId) {
        long start = System.currentTimeMillis();
        String searchType = normalizeType(type);
        List<SearchItemDto> items = new ArrayList<>(domesticSearchProvider.search(keyword, searchType, page, size));
        List<SearchItemDto> ranked = rankingService.rank(keyword, searchType, preferences(userId), items);
        List<SearchItemDto> pageItems = ranked.stream().limit(size).toList();
        boolean hasNext = ranked.size() >= size;
        long estimatedTotal = (long) (page - 1) * size + pageItems.size() + (hasNext ? size : 0);
        Map<String, Long> distribution = ranked.stream()
                .collect(Collectors.groupingBy(SearchItemDto::getType, LinkedHashMap::new, Collectors.counting()));
        saveLog(userId, keyword, searchType, pageItems.size(), System.currentTimeMillis() - start, distribution);
        return new SearchResponse(keyword, searchType, pageItems, estimatedTotal, page, size, hasNext,
                "score = 相关性45% + 时效性20% + 权威性25% + 用户偏好10%", distribution);
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "all";
        }
        String normalized = type.trim().toLowerCase();
        return SUPPORTED_TYPES.contains(normalized) ? normalized : "all";
    }

    private Map<String, Double> preferences(Long userId) {
        if (userId == null) {
            return Map.of();
        }
        UserProfile profile = profileMapper.selectOne(new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId).last("limit 1"));
        if (profile == null || profile.getPreferredTypes() == null) {
            return Map.of();
        }
        Map<String, Double> prefs = new LinkedHashMap<>();
        for (String type : profile.getPreferredTypes().split(",")) {
            if (!type.isBlank()) prefs.put(type.trim(), 1.8);
        }
        return prefs;
    }

    private void saveLog(Long userId, String keyword, String type, int resultCount, long durationMs, Map<String, Long> distribution) {
        SearchLog log = new SearchLog();
        log.setUserId(userId);
        log.setKeyword(keyword);
        log.setType(type);
        log.setResultCount(resultCount);
        log.setDurationMs(durationMs);
        log.setSourceDistribution(distribution.toString());
        searchLogMapper.insert(log);
    }
}
