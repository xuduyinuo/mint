package com.mint.search.content;

import com.mint.search.content.analysis.ContentAnalysisResult;
import com.mint.search.content.analysis.ContentAnalyzer;
import com.mint.search.search.dto.SearchItemDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class ContentPreviewService {
    private final ContentAnalyzer analyzer;

    public ContentPreviewService() {
        this(item -> Optional.empty());
    }

    @Autowired
    public ContentPreviewService(ContentAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public ContentPreviewResponse preview(ContentPreviewRequest request) {
        SearchItemDto item = request.getItem();
        if (item == null) {
            throw new IllegalArgumentException("内容项不能为空");
        }
        String title = value(item.getTitle(), "未命名内容");
        String type = value(item.getType(), "news");
        String sourceName = value(item.getSourceName(), "聚合搜索");
        String summary = value(item.getSummary(), "该内容来自聚合搜索候选结果，当前外部内容源未返回完整正文。");
        String tags = value(item.getTags(), "聚合搜索");
        ContentAnalysisResult analysis = analyze(item)
                .orElseGet(() -> ContentAnalysisResult.fallback(summary, tags, "该内容在当前检索结果中结合了关键词相关性、发布时间、来源权威度和用户偏好等信号进行排序。"));
        String content = analysis.enhanced() ? enhancedContent(title, sourceName, type, analysis) : fallbackContent(title, sourceName, type, summary, tags);
        return new ContentPreviewResponse(title, type, sourceName, item.getUrl(), item.getThumbnailUrl(), content,
                value(analysis.summary(), summary), analysis.tags(), value(analysis.recommendReason(), "结合关键词相关性、发布时间、来源权威度和用户偏好等信号推荐。"), analysis.enhanced());
    }

    private Optional<ContentAnalysisResult> analyze(SearchItemDto item) {
        try {
            return analyzer == null ? Optional.empty() : analyzer.analyze(item);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String fallbackContent(String title, String sourceName, String type, String summary, String tags) {
        return """
                %s

                来源：%s
                类型：%s

                内容摘要：
                %s

                为什么推荐：
                该内容在当前检索结果中结合了关键词相关性、发布时间、来源权威度和用户偏好等信号进行排序。你可以先在这里阅读摘要与推荐依据，再按需打开原文。

                关联标签：
                %s
                """.formatted(title, sourceName, label(type), summary, tags).trim();
    }

    private String enhancedContent(String title, String sourceName, String type, ContentAnalysisResult analysis) {
        String tags = analysis.tags().isEmpty() ? "聚合搜索" : String.join("，", analysis.tags());
        return """
                %s

                来源：%s
                类型：%s

                百炼内容分析：
                %s

                为什么推荐：
                %s

                关联标签：
                %s
                """.formatted(title, sourceName, label(type), value(analysis.summary(), "暂无摘要"),
                value(analysis.recommendReason(), "结合内容质量与用户兴趣推荐。"), tags).trim();
    }

    private String value(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String label(String type) {
        return switch (type) {
            case "news" -> "新闻";
            case "image" -> "图片";
            case "video" -> "视频";
            case "blog" -> "博客";
            default -> "内容";
        };
    }
}
