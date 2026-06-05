package com.mint.search.search.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mint.search.ranking.RankingConfig;
import com.mint.search.ranking.mapper.RankingConfigMapper;
import com.mint.search.search.dto.SearchItemDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RankingService {
    private final RankingConfigMapper rankingMapper;

    public RankingService() {
        this.rankingMapper = null;
    }

    @Autowired
    public RankingService(RankingConfigMapper rankingMapper) {
        this.rankingMapper = rankingMapper;
    }

    public List<SearchItemDto> rank(String keyword, String type, Map<String, Double> userPreferences, List<SearchItemDto> items) {
        String normalized = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT).trim();
        LocalDateTime now = LocalDateTime.now();
        RankingConfig config = activeConfig();
        items.forEach(item -> item.setScore(score(item, normalized, type, userPreferences, now, config)));
        return items.stream()
                .sorted(Comparator.comparing(SearchItemDto::getScore, Comparator.nullsLast(Double::compareTo)).reversed())
                .toList();
    }

    private double score(SearchItemDto item, String keyword, String requestedType,
                         Map<String, Double> userPreferences, LocalDateTime now, RankingConfig config) {
        double relevance = relevance(item, keyword);
        double freshness = freshness(item.getPublishedAt(), now);
        double authority = item.getAuthorityScore() == null ? 0.5 : item.getAuthorityScore();
        double preference = userPreferences.getOrDefault(item.getType(), 1.0);
        double sourceWeight = item.getScore() == null ? 1.0 : item.getScore();
        double typeBoost = StringUtils.hasText(requestedType) && !"all".equals(requestedType) && requestedType.equals(item.getType()) ? 0.3 : 0.0;
        double baseScore = relevance * config.getRelevanceWeight()
                + freshness * config.getFreshnessWeight()
                + authority * config.getAuthorityWeight()
                + preference * config.getPreferenceWeight()
                + typeBoost;
        return baseScore * sourceWeight;
    }

    private RankingConfig activeConfig() {
        if (rankingMapper == null) {
            return fallbackConfig();
        }
        RankingConfig config = rankingMapper.selectOne(new LambdaQueryWrapper<RankingConfig>()
                .eq(RankingConfig::getEnabled, 1)
                .orderByDesc(RankingConfig::getUpdateTime)
                .last("LIMIT 1"));
        if (config != null) {
            return config;
        }
        return fallbackConfig();
    }

    private RankingConfig fallbackConfig() {
        RankingConfig fallback = new RankingConfig();
        fallback.setRelevanceWeight(0.45);
        fallback.setFreshnessWeight(0.2);
        fallback.setAuthorityWeight(0.25);
        fallback.setPreferenceWeight(0.1);
        return fallback;
    }

    private double relevance(SearchItemDto item, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return 0.6;
        }
        String text = ((item.getTitle() == null ? "" : item.getTitle()) + " " +
                (item.getSummary() == null ? "" : item.getSummary()) + " " +
                (item.getTags() == null ? "" : item.getTags())).toLowerCase(Locale.ROOT);
        double score = 0.2;
        for (String token : keyword.split("\\s+")) {
            if (StringUtils.hasText(token) && text.contains(token)) {
                score += 0.35;
            }
        }
        return Math.min(score, 1.0);
    }

    private double freshness(LocalDateTime publishedAt, LocalDateTime now) {
        if (publishedAt == null) {
            return 0.4;
        }
        long hours = Math.max(1, Duration.between(publishedAt, now).toHours());
        if (hours <= 24) {
            return 1.0;
        }
        if (hours <= 24 * 7) {
            return 0.75;
        }
        if (hours <= 24 * 30) {
            return 0.5;
        }
        return 0.25;
    }
}
