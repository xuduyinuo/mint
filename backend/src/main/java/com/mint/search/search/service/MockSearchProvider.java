package com.mint.search.search.service;

import com.mint.search.search.dto.SearchItemDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class MockSearchProvider {
    private static final List<String> TYPES = List.of("news", "image", "video");

    public List<SearchItemDto> search(String keyword, String type) {
        String query = keyword == null || keyword.isBlank() ? "聚合搜索" : keyword.trim();
        List<SearchItemDto> items = new ArrayList<>();
        for (String currentType : TYPES) {
            if (!"all".equalsIgnoreCase(type) && type != null && !type.isBlank() && !currentType.equalsIgnoreCase(type)) {
                continue;
            }
            items.add(item(query, currentType, 1));
            items.add(item(query, currentType, 2));
            items.add(item(query, currentType, 3));
        }
        return items;
    }

    private SearchItemDto item(String keyword, String type, int index) {
        SearchItemDto item = new SearchItemDto();
        item.setExternalId(type + "-" + keyword.hashCode() + "-" + index);
        item.setType(type);
        item.setSourceName(source(type));
        item.setTitle(keyword + " " + label(type) + "精选结果 " + index);
        item.setSummary("来自" + source(type) + "的" + label(type) + "内容，覆盖相关性、时效性、权威性与用户偏好等排序信号。");
        item.setUrl("https://example.com/" + type + "/" + index + "?q=" + keyword);
        item.setThumbnailUrl(thumbnail(type, index));
        item.setTags(keyword + "," + label(type) + ",推荐");
        item.setAuthorityScore(0.62 + index * 0.08);
        item.setPublishedAt(LocalDateTime.now().minusHours(index * 5L + type.length()));
        return item;
    }

    private String label(String type) {
        return switch (type) {
            case "news" -> "新闻";
            case "image" -> "图片";
            case "video" -> "视频";
            default -> "内容";
        };
    }

    private String source(String type) {
        return switch (type) {
            case "news" -> "NewAPI News";
            case "image" -> "NewAPI Image";
            case "video" -> "NewAPI Video";
            default -> "Mint Search";
        };
    }

    private String thumbnail(String type, int index) {
        if ("news".equals(type)) {
            return "";
        }
        return "https://picsum.photos/seed/mint-" + type + "-" + index + "/640/360";
    }
}
