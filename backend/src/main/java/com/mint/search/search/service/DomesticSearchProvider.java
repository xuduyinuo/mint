package com.mint.search.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mint.search.search.dto.SearchItemDto;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class DomesticSearchProvider {
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final int[] BILIBILI_MIXIN_KEY_ENC_TAB = {
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
            27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
            37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
            22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52
    };
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String serpApiKey;
    private final String pexelsApiKey;

    @Autowired
    public DomesticSearchProvider(ObjectMapper objectMapper,
                                  @Value("${serpapi.api-key:}") String serpApiKey,
                                  @Value("${pexels.api-key:}") String pexelsApiKey) {
        this.objectMapper = objectMapper;
        this.serpApiKey = serpApiKey;
        this.pexelsApiKey = pexelsApiKey;
        this.restClient = RestClient.builder()
                .defaultHeader("User-Agent", USER_AGENT)
                .defaultHeader("Accept", "text/html,application/json,*/*")
                .build();
    }

    public DomesticSearchProvider(ObjectMapper objectMapper) {
        this(objectMapper, "", "");
    }

    public List<SearchItemDto> search(String keyword, String type) {
        String searchType = StringUtils.hasText(type) ? type : "all";
        List<SearchItemDto> items = new ArrayList<>();
        if ("all".equalsIgnoreCase(searchType) || "news".equalsIgnoreCase(searchType)) {
            items.addAll(searchTencentNews(keyword));
        }
        if ("all".equalsIgnoreCase(searchType) || "image".equalsIgnoreCase(searchType)) {
            items.addAll(searchPexelsImages(keyword));
        }
        if ("all".equalsIgnoreCase(searchType) || "video".equalsIgnoreCase(searchType)) {
            items.addAll(searchBilibiliVideos(keyword));
        }
        return items;
    }

    private List<SearchItemDto> searchPexelsImages(String keyword) {
        if (!StringUtils.hasText(pexelsApiKey)) {
            return List.of();
        }
        try {
            URI uri = UriComponentsBuilder.fromUriString("https://api.pexels.com/v1/search")
                    .queryParam("query", query(keyword))
                    .queryParam("per_page", "24")
                    .queryParam("page", "1")
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUri();
            String raw = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, pexelsApiKey)
                    .retrieve()
                    .body(String.class);
            return parsePexelsImages(raw, keyword);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<SearchItemDto> searchTencentNews(String keyword) {
        try {
            String raw = restClient.get()
                    .uri(UriComponentsBuilder.fromUriString("https://i.news.qq.com/gw/pc_search/result")
                            .queryParam("page", "0")
                            .queryParam("query", query(keyword))
                            .queryParam("is_pc", "1")
                            .queryParam("hippy_custom_version", "25")
                            .queryParam("search_type", "all")
                            .queryParam("search_count_limit", "10")
                            .queryParam("appver", "15.5_qqnews_7.1.80")
                            .build()
                            .encode(StandardCharsets.UTF_8)
                            .toUri())
                    .header("Referer", UriComponentsBuilder.fromUriString("https://news.qq.com/search")
                            .queryParam("query", query(keyword))
                            .encode(StandardCharsets.UTF_8)
                            .toUriString())
                    .header("Origin", "https://news.qq.com")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126 Safari/537.36")
                    .retrieve()
                    .body(String.class);
            List<SearchItemDto> items = parseTencentNews(raw, keyword);
            if (!items.isEmpty()) {
                return items;
            }
        } catch (Exception ignored) {
        }
        return searchGoogleNewsRss(keyword);
    }

    private List<SearchItemDto> searchGoogleNewsRss(String keyword) {
        try {
            String raw = restClient.get()
                    .uri(UriComponentsBuilder.fromUriString("https://news.google.com/rss/search")
                            .queryParam("q", query(keyword))
                            .queryParam("hl", "zh-CN")
                            .queryParam("gl", "CN")
                            .queryParam("ceid", "CN:zh-Hans")
                            .build()
                            .encode(StandardCharsets.UTF_8)
                            .toUri())
                    .retrieve()
                    .body(String.class);
            return parseGoogleNewsRss(raw, keyword);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<SearchItemDto> searchBilibiliVideos(String keyword) {
        try {
            String raw = restClient.get()
                    .uri(UriComponentsBuilder.fromUriString("https://api.bilibili.com/x/web-interface/search/type")
                            .queryParam("search_type", "video")
                            .queryParam("keyword", query(keyword))
                            .queryParam("page", "1")
                            .queryParam("page_size", "12")
                            .build()
                            .encode(StandardCharsets.UTF_8)
                            .toUri())
                    .header("Referer", "https://search.bilibili.com/")
                    .retrieve()
                    .body(String.class);
            List<SearchItemDto> items = parseBilibiliVideos(raw, keyword);
            if (!items.isEmpty()) {
                return items;
            }
        } catch (Exception ignored) {
        }
        try {
            String raw = restClient.get()
                    .uri(buildBilibiliVideoSearchUri(keyword))
                    .header("Referer", "https://search.bilibili.com/")
                    .retrieve()
                    .body(String.class);
            return parseBilibiliVideos(raw, keyword);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<SearchItemDto> searchSerpApiYoutubeVideos(String keyword) {
        if (!StringUtils.hasText(serpApiKey)) {
            return List.of();
        }
        try {
            String raw = restClient.get()
                    .uri(buildSerpApiYoutubeSearchUri(keyword, serpApiKey))
                    .retrieve()
                    .body(String.class);
            return parseSerpApiYoutubeVideos(raw, keyword);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    public List<SearchItemDto> parsePexelsImages(String json, String keyword) throws Exception {
        JsonNode photos = objectMapper.readTree(json).path("photos");
        List<SearchItemDto> items = new ArrayList<>();
        if (!photos.isArray()) {
            return items;
        }
        for (JsonNode node : photos) {
            String thumbnailUrl = firstText(node.path("src"), "medium", "large", "original");
            String url = node.path("url").asText("");
            if (!StringUtils.hasText(thumbnailUrl) || !StringUtils.hasText(url)) {
                continue;
            }
            String photographer = node.path("photographer").asText("");
            String title = textOrDefault(node.path("alt").asText(""), query(keyword) + " 图片");
            SearchItemDto item = baseItem("image", "Pexels", keyword, title, items.size() + 1);
            item.setUrl(url);
            item.setThumbnailUrl(thumbnailUrl);
            item.setSummary("Pexels 图片，摄影师：" + textOrDefault(photographer, "未知") + "，关键词：" + query(keyword));
            item.setPublishedAt(LocalDateTime.now());
            items.add(item);
        }
        return items.stream().limit(24).toList();
    }

    public List<SearchItemDto> parseTencentNews(String json, String keyword) throws Exception {
        JsonNode sections = objectMapper.readTree(json).path("secList");
        List<SearchItemDto> items = new ArrayList<>();
        if (!sections.isArray()) {
            return items;
        }
        for (JsonNode section : sections) {
            JsonNode newsList = section.path("newsList");
            if (!newsList.isArray()) {
                continue;
            }
            for (JsonNode node : newsList) {
                String title = cleanHtml(firstText(node, "title", "longtitle"));
                String url = firstText(node, "url", "surl", "short_url");
                if (!StringUtils.hasText(title) || !StringUtils.hasText(url)) {
                    continue;
                }
                SearchItemDto item = baseItem("news", sourceName(node), keyword, title, items.size() + 1);
                item.setUrl(url);
                item.setSummary(textOrDefault(cleanHtml(node.path("abstract").asText("")), "腾讯新闻搜索结果，关键词：" + query(keyword)));
                JsonNode thumbnails = node.path("thumbnails_qqnews");
                item.setThumbnailUrl(thumbnails.isArray() && !thumbnails.isEmpty() ? thumbnails.get(0).asText("") : "");
                item.setPublishedAt(parseTencentTime(node));
                items.add(item);
            }
        }
        return items.stream().limit(24).toList();
    }

    public List<SearchItemDto> parseGoogleNewsRss(String xml, String keyword) {
        List<SearchItemDto> items = new ArrayList<>();
        Jsoup.parse(xml, "", Parser.xmlParser()).select("item").forEach(element -> {
            String title = element.selectFirst("title") == null ? "" : element.selectFirst("title").text();
            String url = element.selectFirst("link") == null ? "" : element.selectFirst("link").text();
            if (!StringUtils.hasText(title) || !StringUtils.hasText(url)) {
                return;
            }
            String sourceName = element.selectFirst("source") == null ? "Google 新闻" : element.selectFirst("source").text();
            String description = element.selectFirst("description") == null ? "" : cleanHtml(element.selectFirst("description").text());
            SearchItemDto item = baseItem("news", textOrDefault(sourceName, "Google 新闻"), keyword, title, items.size() + 1);
            item.setUrl(url);
            item.setSummary(textOrDefault(description, "Google 新闻搜索结果，关键词：" + query(keyword)));
            item.setThumbnailUrl("");
            item.setPublishedAt(parseRssDate(element.selectFirst("pubDate") == null ? "" : element.selectFirst("pubDate").text()));
            items.add(item);
        });
        return items.stream().limit(24).toList();
    }

    public List<SearchItemDto> parseBilibiliVideos(String json, String keyword) throws Exception {
        JsonNode results = objectMapper.readTree(json).path("data").path("result");
        List<SearchItemDto> items = new ArrayList<>();
        if (!results.isArray()) {
            return items;
        }
        for (JsonNode node : results) {
            String title = cleanHtml(node.path("title").asText(""));
            if (!StringUtils.hasText(title)) {
                continue;
            }
            SearchItemDto item = baseItem("video", textOrDefault(node.path("author").asText(""), "B站"), keyword, title, items.size() + 1);
            item.setUrl(firstText(node, "arcurl", "url"));
            item.setSummary(textOrDefault(cleanHtml(node.path("description").asText("")), "B站视频搜索结果，关键词：" + query(keyword)));
            item.setThumbnailUrl(mediaProxyUrl(normalizeUrl(node.path("pic").asText(""))));
            item.setPublishedAt(parseUnix(node.path("pubdate").asLong(0)));
            items.add(item);
        }
        return items.stream().limit(24).toList();
    }

    public List<SearchItemDto> parseSerpApiYoutubeVideos(String json, String keyword) throws Exception {
        JsonNode results = objectMapper.readTree(json).path("video_results");
        List<SearchItemDto> items = new ArrayList<>();
        if (!results.isArray()) {
            return items;
        }
        for (JsonNode node : results) {
            String title = node.path("title").asText("");
            String url = firstText(node, "link", "url");
            if (!StringUtils.hasText(title) || !StringUtils.hasText(url)) {
                continue;
            }
            SearchItemDto item = baseItem("video", serpApiYoutubeSource(node), keyword, title, items.size() + 1);
            item.setUrl(url);
            item.setSummary(textOrDefault(node.path("snippet").asText(""), "YouTube 视频搜索结果，关键词：" + query(keyword)));
            item.setThumbnailUrl(serpApiYoutubeThumbnail(node.path("thumbnail")));
            item.setPublishedAt(LocalDateTime.now());
            items.add(item);
        }
        return items.stream().limit(24).toList();
    }

    private SearchItemDto baseItem(String type, String sourceName, String keyword, String title, int position) {
        SearchItemDto item = new SearchItemDto();
        item.setExternalId("domestic-" + type + "-" + position + "-" + title.hashCode());
        item.setType(type);
        item.setSourceName(sourceName);
        item.setTitle(title);
        item.setTags(query(keyword) + "," + sourceName);
        item.setAuthorityScore(0.82);
        return item;
    }

    private LocalDateTime parseTencentTime(JsonNode node) {
        String time = node.path("time").asText("");
        if (StringUtils.hasText(time)) {
            try {
                return LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ignored) {
            }
        }
        long timestamp = node.path("timestamp").asLong(0);
        return parseUnix(timestamp);
    }

    private LocalDateTime parseUnix(long timestamp) {
        if (timestamp <= 0) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
    }

    private LocalDateTime parseRssDate(String value) {
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now();
        }
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDateTime();
        } catch (Exception ignored) {
            return LocalDateTime.now();
        }
    }

    private String sourceName(JsonNode node) {
        String source = firstText(node, "uinnick", "chlname", "source");
        if (!StringUtils.hasText(source)) {
            source = node.path("card").path("chlname").asText("");
        }
        return StringUtils.hasText(source) ? source : "腾讯新闻";
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText("");
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private URI buildBilibiliVideoSearchUri(String keyword) throws Exception {
        JsonNode nav = objectMapper.readTree(restClient.get()
                .uri("https://api.bilibili.com/x/web-interface/nav")
                .retrieve()
                .body(String.class));
        String imgKey = fileKey(nav.path("data").path("wbi_img").path("img_url").asText(""));
        String subKey = fileKey(nav.path("data").path("wbi_img").path("sub_url").asText(""));
        String mixinKey = mixinKey(imgKey + subKey);
        Map<String, String> params = new TreeMap<>();
        params.put("keyword", query(keyword));
        params.put("page", "1");
        params.put("page_size", "12");
        params.put("search_type", "video");
        params.put("wts", String.valueOf(Instant.now().getEpochSecond()));
        String queryString = queryString(params);
        String wRid = md5(queryString + mixinKey);
        return URI.create("https://api.bilibili.com/x/web-interface/wbi/search/type?" + queryString + "&w_rid=" + wRid);
    }

    public URI buildSerpApiYoutubeSearchUri(String keyword, String apiKey) {
        return UriComponentsBuilder.fromUriString("https://serpapi.com/search")
                .queryParam("engine", "youtube")
                .queryParam("search_query", query(keyword))
                .queryParam("api_key", apiKey)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
    }

    private String serpApiYoutubeSource(JsonNode node) {
        String channel = node.path("channel").path("name").asText("");
        return StringUtils.hasText(channel) ? channel : "YouTube";
    }

    private String serpApiYoutubeThumbnail(JsonNode thumbnail) {
        if (thumbnail.isTextual()) {
            return thumbnail.asText("");
        }
        return firstText(thumbnail, "static", "rich", "url");
    }

    private String fileKey(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        int dot = fileName.indexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private String mixinKey(String rawKey) {
        StringBuilder builder = new StringBuilder();
        for (int index : BILIBILI_MIXIN_KEY_ENC_TAB) {
            if (index < rawKey.length()) {
                builder.append(rawKey.charAt(index));
            }
        }
        return builder.substring(0, Math.min(32, builder.length()));
    }

    private String queryString(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue().replaceAll("[!'()*]", "")))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String md5(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String cleanHtml(String value) {
        return Jsoup.parse(value == null ? "" : value).text();
    }

    private String normalizeUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.startsWith("//") ? "https:" + value : value;
    }

    private String mediaProxyUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }
        return "/api/media/thumbnail?url=" + encode(url);
    }

    private String query(String keyword) {
        return StringUtils.hasText(keyword) ? keyword : "今日热点";
    }

    private String textOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }
}
