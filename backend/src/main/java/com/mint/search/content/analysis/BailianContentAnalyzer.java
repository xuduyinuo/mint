package com.mint.search.content.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mint.search.search.dto.SearchItemDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class BailianContentAnalyzer implements ContentAnalyzer {
    private final BailianProperties properties;
    private final ObjectMapper objectMapper;

    public BailianContentAnalyzer(BailianProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ContentAnalysisResult> analyze(SearchItemDto item) {
        if (item == null || !StringUtils.hasText(properties.getApiKey())) {
            return Optional.empty();
        }
        try {
            RestClient client = RestClient.builder()
                    .baseUrl(trimTrailingSlash(properties.getBaseUrl()))
                    .requestFactory(requestFactory())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
            String response = client.post()
                    .uri("/chat/completions")
                    .body(requestBody(item))
                    .retrieve()
                    .body(String.class);
            return parseResponse(response);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    Optional<ContentAnalysisResult> parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (!StringUtils.hasText(content)) {
                return Optional.empty();
            }
            JsonNode payload = objectMapper.readTree(stripCodeFence(content));
            String summary = payload.path("summary").asText("");
            String reason = payload.path("recommendReason").asText(payload.path("recommend_reason").asText(""));
            List<String> tags = tags(payload.path("tags"));
            if (!StringUtils.hasText(summary) && !StringUtils.hasText(reason) && tags.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new ContentAnalysisResult(summary, tags, reason, true));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getTimeoutMs());
        factory.setReadTimeout(properties.getTimeoutMs());
        return factory;
    }

    private Map<String, Object> requestBody(SearchItemDto item) {
        String prompt = """
                请分析以下聚合搜索内容，返回严格 JSON，不要输出 Markdown：
                {
                  "summary": "80字以内中文摘要",
                  "tags": ["3到6个中文标签"],
                  "recommendReason": "一句话说明推荐理由"
                }

                标题：%s
                类型：%s
                来源：%s
                摘要：%s
                标签：%s
                """.formatted(value(item.getTitle()), value(item.getType()), value(item.getSourceName()),
                value(item.getSummary()), value(item.getTags()));
        return Map.of(
                "model", properties.getModel(),
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", "你是 Mint 聚合搜索平台的内容分析助手，只返回可解析 JSON。"),
                        Map.of("role", "user", "content", prompt)
                )
        );
    }

    private List<String> tags(JsonNode node) {
        Set<String> values = new LinkedHashSet<>();
        if (node.isArray()) {
            node.forEach(value -> addTag(values, value.asText("")));
        } else {
            for (String value : node.asText("").split(",")) {
                addTag(values, value);
            }
        }
        return values.stream().limit(8).toList();
    }

    private void addTag(Set<String> values, String value) {
        if (StringUtils.hasText(value)) {
            values.add(value.trim());
        }
    }

    private String stripCodeFence(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed.trim();
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String value(String value) {
        return StringUtils.hasText(value) ? value : "无";
    }
}
