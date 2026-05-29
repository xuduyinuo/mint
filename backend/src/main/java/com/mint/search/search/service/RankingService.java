package com.mint.search.search.service;

import com.mint.search.search.dto.SearchItemDto;
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
    public List<SearchItemDto> rank(String keyword, String type, Map<String, Double> userPreferences, List<SearchItemDto> items) {
        String normalized = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT).trim();
        LocalDateTime now = LocalDateTime.now();
        items.forEach(item -> item.setScore(score(item, normalized, type, userPreferences, now)));
        return items.stream()
                .sorted(Comparator.comparing(SearchItemDto::getScore, Comparator.nullsLast(Double::compareTo)).reversed())
                .toList();
    }

    private double score(SearchItemDto item, String keyword, String requestedType,
                         Map<String, Double> userPreferences, LocalDateTime now) {
        double relevance = relevance(item, keyword);
        double freshness = freshness(item.getPublishedAt(), now);
        double authority = item.getAuthorityScore() == null ? 0.5 : item.getAuthorityScore();
        double preference = userPreferences.getOrDefault(item.getType(), 1.0);
        double typeBoost = StringUtils.hasText(requestedType) && !"all".equals(requestedType) && requestedType.equals(item.getType()) ? 0.3 : 0.0;
        return relevance * 0.45 + freshness * 0.2 + authority * 0.25 + preference * 0.1 + typeBoost;
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
