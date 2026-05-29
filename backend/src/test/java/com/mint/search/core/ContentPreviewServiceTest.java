package com.mint.search.core;

import com.mint.search.content.ContentPreviewRequest;
import com.mint.search.content.ContentPreviewService;
import com.mint.search.search.dto.SearchItemDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContentPreviewServiceTest {
    @Test
    void buildsReadableFallbackContentForSearchItem() {
        ContentPreviewService service = new ContentPreviewService();
        SearchItemDto item = new SearchItemDto();
        item.setTitle("AI 搜索 新闻精选结果");
        item.setType("news");
        item.setSourceName("NewAPI News");
        item.setSummary("覆盖相关性、时效性、权威性与用户偏好。");
        item.setUrl("https://example.com/news/1");
        item.setTags("AI,新闻,推荐");

        var preview = service.preview(new ContentPreviewRequest(item));

        assertThat(preview.getTitle()).isEqualTo("AI 搜索 新闻精选结果");
        assertThat(preview.getContent()).contains("覆盖相关性", "为什么推荐", "AI,新闻,推荐");
        assertThat(preview.getOriginalUrl()).isEqualTo("https://example.com/news/1");
    }
}
