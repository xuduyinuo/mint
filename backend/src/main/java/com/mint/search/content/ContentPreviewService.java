package com.mint.search.content;

import com.mint.search.search.dto.SearchItemDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ContentPreviewService {
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
        String content = """
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
        return new ContentPreviewResponse(title, type, sourceName, item.getUrl(), item.getThumbnailUrl(), content);
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
