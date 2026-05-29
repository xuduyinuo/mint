package com.mint.search.core;

import com.mint.search.hot.HotDataService;
import com.mint.search.recommendation.RecommendationService;
import com.mint.search.search.dto.SearchItemDto;
import com.mint.search.search.service.RankingService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendationServiceTest {
    @Test
    void anonymousRecommendationsUseHotData() {
        SearchItemDto item = new SearchItemDto();
        item.setTitle("热点新闻");
        item.setType("news");
        item.setSourceName("热点缓存");
        item.setUrl("https://example.com/hot");
        HotDataService hotDataService = mock(HotDataService.class);
        when(hotDataService.getHotData("all")).thenReturn(List.of(item));

        RecommendationService service = new RecommendationService(
                null,
                hotDataService,
                new RankingService()
        );

        var recommendations = service.recommend(null, "all");

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.getFirst().getSourceName()).isEqualTo("热点缓存");
    }

    @Test
    void returnsEmptyWhenHotDataIsEmpty() {
        HotDataService hotDataService = mock(HotDataService.class);
        when(hotDataService.getHotData("all")).thenReturn(List.of());

        RecommendationService service = new RecommendationService(
                null,
                hotDataService,
                new RankingService()
        );

        assertThat(service.recommend(null, "all")).isEmpty();
    }
}
