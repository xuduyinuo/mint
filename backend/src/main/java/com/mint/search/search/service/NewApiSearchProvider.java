package com.mint.search.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mint.search.search.dto.SearchItemDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class NewApiSearchProvider {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public NewApiSearchProvider(ObjectMapper objectMapper,
                                @Value("${newapi.base-url:}") String baseUrl,
                                @Value("${newapi.api-key:}") String apiKey,
                                @Value("${newapi.model:gpt-4.1-mini}") String model) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder().build();
    }

    public List<SearchItemDto> search(String keyword, String type) {
        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(apiKey)) {
            return List.of();
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt(keyword, type))),
                    "temperature", 0.2
            );
            String raw = restClient.post()
                    .uri(baseUrl.endsWith("/") ? baseUrl + "v1/chat/completions" : baseUrl + "/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parse(raw, keyword);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String prompt(String keyword, String type) {
        return "请为聚合搜索关键词\"" + keyword + "\"生成" + type +
                "类型候选结果JSON数组，每项包含type,sourceName,title,summary,url,thumbnailUrl,tags,authorityScore。";
    }

    private List<SearchItemDto> parse(String raw, String keyword) throws Exception {
        JsonNode root = objectMapper.readTree(raw);
        String content = root.at("/choices/0/message/content").asText("[]");
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start >= 0 && end > start) {
            content = content.substring(start, end + 1);
        }
        JsonNode array = objectMapper.readTree(content);
        List<SearchItemDto> items = new ArrayList<>();
        if (array.isArray()) {
            int index = 0;
            for (JsonNode node : array) {
                SearchItemDto item = new SearchItemDto();
                item.setExternalId("newapi-" + keyword.hashCode() + "-" + index++);
                item.setType(node.path("type").asText("news"));
                item.setSourceName(node.path("sourceName").asText("NewAPI"));
                item.setTitle(node.path("title").asText(keyword));
                item.setSummary(node.path("summary").asText(""));
                item.setUrl(node.path("url").asText("https://example.com"));
                item.setThumbnailUrl(node.path("thumbnailUrl").asText(""));
                item.setTags(node.path("tags").asText(keyword));
                item.setAuthorityScore(node.path("authorityScore").asDouble(0.7));
                item.setPublishedAt(LocalDateTime.now());
                items.add(item);
            }
        }
        return items;
    }
}
