package com.mint.search.core;

import com.mint.search.content.ContentPreviewRequest;
import com.mint.search.content.ContentPreviewService;
import com.mint.search.content.analysis.ContentAnalysisResult;
import com.mint.search.search.dto.SearchItemDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

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
        assertThat(preview.isLlmEnhanced()).isFalse();
        assertThat(preview.getAnalysisSummary()).contains("覆盖相关性");
    }

    @Test
    void usesAnalyzerResultWhenAvailable() {
        ContentPreviewService service = new ContentPreviewService(item -> Optional.of(new ContentAnalysisResult(
                "这是一条由百炼生成的精炼摘要。",
                List.of("AI", "搜索", "推荐"),
                "它匹配你的搜索兴趣和近期点击内容。",
                true
        )));
        SearchItemDto item = new SearchItemDto();
        item.setTitle("AI 搜索 新闻精选结果");
        item.setType("news");
        item.setSourceName("聚合新闻");
        item.setSummary("原始摘要");
        item.setUrl("https://example.com/news/1");

        var preview = service.preview(new ContentPreviewRequest(item));

        assertThat(preview.isLlmEnhanced()).isTrue();
        assertThat(preview.getAnalysisSummary()).isEqualTo("这是一条由百炼生成的精炼摘要。");
        assertThat(preview.getAnalysisTags()).containsExactly("AI", "搜索", "推荐");
        assertThat(preview.getRecommendReason()).isEqualTo("它匹配你的搜索兴趣和近期点击内容。");
        assertThat(preview.getContent()).contains("百炼内容分析", "这是一条由百炼生成的精炼摘要。", "它匹配你的搜索兴趣和近期点击内容。");
    }

    @Test
    void fallsBackWhenAnalyzerFails() {
        ContentPreviewService service = new ContentPreviewService(item -> {
            throw new IllegalStateException("bailian unavailable");
        });
        SearchItemDto item = new SearchItemDto();
        item.setTitle("AI 搜索 新闻精选结果");
        item.setType("news");
        item.setSourceName("聚合新闻");
        item.setSummary("原始摘要");

        var preview = service.preview(new ContentPreviewRequest(item));

        assertThat(preview.isLlmEnhanced()).isFalse();
        assertThat(preview.getAnalysisSummary()).isEqualTo("原始摘要");
        assertThat(preview.getContent()).contains("原始摘要", "为什么推荐");
    }
}
