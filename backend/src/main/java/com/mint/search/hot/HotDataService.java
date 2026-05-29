package com.mint.search.hot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mint.search.search.dto.SearchItemDto;
import com.mint.search.search.service.DomesticSearchProvider;
import com.mint.search.search.service.RankingService;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class HotDataService {
    private static final String HOT_KEYWORD = "今日热点";
    private static final String KEY_PREFIX = "mint:hot:";
    private static final List<String> HOT_TYPES = List.of("news", "image", "video");
    private static final TypeReference<List<SearchItemDto>> ITEM_LIST_TYPE = new TypeReference<>() {
    };

    private final DomesticSearchProvider domesticSearchProvider;
    private final RankingService rankingService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public HotDataService(DomesticSearchProvider domesticSearchProvider, RankingService rankingService,
                          StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.domesticSearchProvider = domesticSearchProvider;
        this.rankingService = rankingService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void warmUp() {
        refreshHotData();
    }

    @Scheduled(fixedRateString = "${hot.refresh-interval-ms:3600000}")
    public void refreshHotData() {
        List<SearchItemDto> allItems = new ArrayList<>();
        for (String type : HOT_TYPES) {
            List<SearchItemDto> items = rankingService.rank(HOT_KEYWORD, type, Map.of(),
                    domesticSearchProvider.search(HOT_KEYWORD, type)).stream().limit(12).toList();
            if (!items.isEmpty()) {
                cache(type, items);
                allItems.addAll(items);
            }
        }
        List<SearchItemDto> allHot = rankingService.rank(HOT_KEYWORD, "all", Map.of(), allItems).stream().limit(24).toList();
        if (!allHot.isEmpty()) {
            cache("all", allHot);
        }
    }

    public List<SearchItemDto> getHotData(String type) {
        String normalized = normalizeType(type);
        List<SearchItemDto> cached = readCache(normalized);
        if (!cached.isEmpty()) {
            return cached;
        }
        refreshHotData();
        cached = readCache(normalized);
        if (!cached.isEmpty()) {
            return cached;
        }
        return rankingService.rank(HOT_KEYWORD, normalized, Map.of(),
                domesticSearchProvider.search(HOT_KEYWORD, normalized)).stream().limit("all".equals(normalized) ? 24 : 12).toList();
    }

    private void cache(String type, List<SearchItemDto> items) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + type, objectMapper.writeValueAsString(items), Duration.ofHours(2));
        } catch (Exception ignored) {
        }
    }

    private List<SearchItemDto> readCache(String type) {
        try {
            String raw = redisTemplate.opsForValue().get(KEY_PREFIX + type);
            return raw == null || raw.isBlank() ? List.of() : objectMapper.readValue(raw, ITEM_LIST_TYPE);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String normalizeType(String type) {
        return HOT_TYPES.contains(type) ? type : "all";
    }
}
