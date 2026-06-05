package com.mint.search.core;

import com.mint.search.search.dto.SearchItemDto;
import com.mint.search.search.service.RankingService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SearchRankingServiceTest {

    @Test
    void ranksByKeywordFreshnessAuthorityAndUserPreference() {
        RankingService rankingService = new RankingService();
        List<SearchItemDto> ranked = rankingService.rank("AI 搜索", "news", Map.of("news", 1.8), List.of(
                item("old-video", "旧视频资料", "video", 0.8, LocalDateTime.now().minusDays(20)),
                item("fresh-news", "AI 搜索 最新进展", "news", 0.9, LocalDateTime.now().minusHours(2)),
                item("low-authority", "AI 搜索 社区帖子", "news", 0.1, LocalDateTime.now().minusHours(1))
        ));

        assertThat(ranked).extracting(SearchItemDto::getTitle)
                .containsExactly("AI 搜索 最新进展", "AI 搜索 社区帖子", "旧视频资料");
        assertThat(ranked.getFirst().getScore()).isGreaterThan(ranked.getLast().getScore());
    }

    @Test
    void injectedQueryTagsDoNotMakeUnrelatedItemsLookRelevant() {
        RankingService rankingService = new RankingService();
        SearchItemDto unrelated = item("unrelated", "油价调整公告", "news", 0.9, LocalDateTime.now().minusHours(1));
        unrelated.setSummary("多家航司发布燃油附加费调整公告。");
        unrelated.setTags("腾讯新闻");
        SearchItemDto related = item("related", "库里生涯首冠赛季集锦", "video", 0.8, LocalDateTime.now().minusDays(5));

        List<SearchItemDto> ranked = rankingService.rank("库里", "all", Map.of(), List.of(unrelated, related));

        assertThat(ranked.getFirst().getExternalId()).isEqualTo("related");
    }

    private SearchItemDto item(String id, String title, String type, double authority, LocalDateTime publishedAt) {
        SearchItemDto item = new SearchItemDto();
        item.setExternalId(id);
        item.setTitle(title);
        item.setType(type);
        item.setAuthorityScore(authority);
        item.setPublishedAt(publishedAt);
        return item;
    }
}
