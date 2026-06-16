package com.mint.search.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mint.search.search.dto.SearchItemDto;
import com.mint.search.source.SearchSource;
import com.mint.search.source.mapper.SearchSourceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
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
    private final SearchSourceMapper sourceMapper;
    private final String serpApiKey;
    private final String pexelsApiKey;

    @Autowired
    public DomesticSearchProvider(ObjectMapper objectMapper,
                                  SearchSourceMapper sourceMapper,
                                  @Value("${serpapi.api-key:}") String serpApiKey,
                                  @Value("${pexels.api-key:}") String pexelsApiKey) {
        this.objectMapper = objectMapper;
        this.sourceMapper = sourceMapper;
        this.serpApiKey = serpApiKey;
        this.pexelsApiKey = pexelsApiKey;
        this.restClient = RestClient.builder()
                .defaultHeader("User-Agent", USER_AGENT)
                .defaultHeader("Accept", "text/html,application/json,*/*")
                .build();
    }

    public DomesticSearchProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.sourceMapper = null;
        this.serpApiKey = "";
        this.pexelsApiKey = "";
        this.restClient = RestClient.builder()
                .defaultHeader("User-Agent", USER_AGENT)
                .defaultHeader("Accept", "text/html,application/json,*/*")
                .build();
    }

    public List<SearchItemDto> search(String keyword, String type) {
        return search(keyword, type, 1, 12);
    }

    public List<SearchItemDto> search(String keyword, String type, int page, int size) {
        String searchType = StringUtils.hasText(type) ? type : "all";
        int searchPage = Math.max(page, 1);
        int pageSize = Math.min(Math.max(size, 1), 16);
        List<SearchItemDto> items = new ArrayList<>();
        for (SourceRuntime source : activeSources(searchType)) {
            List<SearchItemDto> sourceItems = switch (source.provider()) {
                case "tencent_news" -> searchTencentNews(keyword, searchPage, pageSize);
                case "google_news_rss" -> searchGoogleNewsRss(keyword, searchPage, pageSize);
                case "pexels" -> searchPexelsImages(keyword, searchPage, pageSize, source.apiKey(pexelsApiKey));
                case "bilibili" -> searchBilibiliVideos(keyword, searchPage, pageSize);
                case "serpapi_youtube" -> searchSerpApiYoutubeVideos(keyword, source.apiKey(serpApiKey));
                default -> List.of();
            };
            items.addAll(applySourceConfig(sourceItems, source));
        }
        return items;
    }

    private List<SearchItemDto> searchPexelsImages(String keyword, int page, int size) {
        return searchPexelsImages(keyword, page, size, pexelsApiKey);
    }

    private List<SearchItemDto> searchPexelsImages(String keyword, int page, int size, String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return List.of();
        }
        try {
            URI uri = UriComponentsBuilder.fromUriString("https://api.pexels.com/v1/search")
                    .queryParam("query", query(keyword))
                    .queryParam("per_page", String.valueOf(size))
                    .queryParam("page", String.valueOf(page))
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUri();
            String raw = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, apiKey)
                    .retrieve()
                    .body(String.class);
            return parsePexelsImages(raw, keyword);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<SearchItemDto> searchTencentNews(String keyword, int page, int size) {
        if (isHotKeyword(keyword)) {
            List<SearchItemDto> hotItems = searchTencentHotNews(page, size);
            if (!hotItems.isEmpty()) {
                return hotItems;
            }
        }
        int from = Math.max(page - 1, 0) * size;
        int target = from + size;
        List<SearchItemDto> items = new ArrayList<>();
        String searchId = "";
        boolean hasMore = true;
        for (int sourcePage = 0; hasMore && items.size() < target && sourcePage < page * 5 + 5; sourcePage++) {
            TencentNewsPage newsPage = fetchTencentNewsPage(keyword, sourcePage, size, searchId);
            if (newsPage.items().isEmpty() && sourcePage == 0) {
                break;
            }
            items.addAll(newsPage.items());
            searchId = newsPage.searchId();
            hasMore = newsPage.hasMore() && StringUtils.hasText(searchId);
        }
        if (items.size() > from) {
            return items.stream().skip(from).limit(size).toList();
        }
        return searchGoogleNewsRss(keyword, page, size);
    }

    private List<SearchItemDto> searchTencentHotNews(int page, int size) {
        try {
            String raw = restClient.get()
                    .uri(UriComponentsBuilder.fromUriString("https://i.news.qq.com/web_feed/getHotQaChannelRankList")
                            .queryParam("rank_id", "thing_hot_rank_qa_channel_realtime")
                            .queryParam("page", String.valueOf(Math.max(page, 1)))
                            .queryParam("size", String.valueOf(Math.max(size, 12)))
                            .queryParam("id_hash", "")
                            .build()
                            .encode(StandardCharsets.UTF_8)
                            .toUri())
                    .header("Referer", "https://news.qq.com/")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126 Safari/537.36")
                    .retrieve()
                    .body(String.class);
            return parseTencentHotNews(raw, "今日热点").stream().limit(size).toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private TencentNewsPage fetchTencentNewsPage(String keyword, int page, int size, String searchId) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("https://i.news.qq.com/gw/pc_search/result")
                    .queryParam("page", String.valueOf(page))
                    .queryParam("query", encode(query(keyword)))
                    .queryParam("is_pc", "1")
                    .queryParam("hippy_custom_version", "25")
                    .queryParam("search_type", "all")
                    .queryParam("search_count_limit", String.valueOf(Math.max(size, 12)))
                    .queryParam("appver", "15.5_qqnews_7.1.80");
            if (StringUtils.hasText(searchId)) {
                builder.queryParam("search_id", searchId);
            }
            String raw = restClient.get()
                    .uri(builder.build().encode(StandardCharsets.UTF_8).toUri())
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
            JsonNode root = objectMapper.readTree(raw);
            return new TencentNewsPage(
                    parseTencentNews(raw, keyword),
                    root.path("search_id").asText(""),
                    root.path("hasMore").asInt(0) == 1
            );
        } catch (Exception ignored) {
            return new TencentNewsPage(List.of(), "", false);
        }
    }

    private List<SearchItemDto> searchGoogleNewsRss(String keyword, int page, int size) {
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
            int from = Math.max(page - 1, 0) * size;
            return parseGoogleNewsRss(raw, keyword).stream().skip(from).limit(size).toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<SearchItemDto> searchBilibiliVideos(String keyword, int page, int size) {
        try {
            String raw = restClient.get()
                    .uri(UriComponentsBuilder.fromUriString("https://api.bilibili.com/x/web-interface/search/type")
                            .queryParam("search_type", "video")
                            .queryParam("keyword", query(keyword))
                            .queryParam("page", String.valueOf(page))
                            .queryParam("page_size", String.valueOf(size))
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
                    .uri(buildBilibiliVideoSearchUri(keyword, page, size))
                    .header("Referer", "https://search.bilibili.com/")
                    .retrieve()
                    .body(String.class);
            return parseBilibiliVideos(raw, keyword);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<SearchItemDto> searchSerpApiYoutubeVideos(String keyword) {
        return searchSerpApiYoutubeVideos(keyword, serpApiKey);
    }

    private List<SearchItemDto> searchSerpApiYoutubeVideos(String keyword, String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return List.of();
        }
        try {
            String raw = restClient.get()
                    .uri(buildSerpApiYoutubeSearchUri(keyword, apiKey))
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
            String title = textOrDefault(node.path("alt").asText(""), "Pexels 图片");
            SearchItemDto item = baseItem("image", "Pexels", keyword, title, items.size() + 1);
            item.setUrl(url);
            item.setThumbnailUrl(thumbnailUrl);
            item.setSummary("Pexels 图片，摄影师：" + textOrDefault(photographer, "未知"));
            item.setPublishedAt(LocalDateTime.now());
            items.add(item);
        }
        return items.stream().limit(24).toList();
    }

    private List<SourceRuntime> activeSources(String type) {
        if (sourceMapper == null) {
            return defaultSources().stream()
                    .filter(source -> supportsType(source, type))
                    .toList();
        }
        List<SearchSource> rows = sourceMapper.selectList(new LambdaQueryWrapper<SearchSource>()
                .eq(SearchSource::getEnabled, 1)
                .orderByDesc(SearchSource::getWeight)
                .orderByDesc(SearchSource::getAuthorityScore));
        List<SourceRuntime> sources = rows.stream()
                .map(this::toRuntime)
                .filter(source -> supportsType(source, type))
                .sorted(Comparator.comparing(SourceRuntime::weight).reversed())
                .toList();
        if (!sources.isEmpty()) {
            return sources;
        }
        return defaultSources().stream()
                .filter(source -> supportsType(source, type))
                .toList();
    }

    private boolean supportsType(SourceRuntime source, String type) {
        return "all".equalsIgnoreCase(type) || source.type().equalsIgnoreCase(type);
    }

    private SourceRuntime toRuntime(SearchSource source) {
        Map<String, String> config = parseConfig(source.getConfigJson());
        String provider = config.getOrDefault("provider", providerFromName(source));
        return new SourceRuntime(
                source.getName(),
                source.getType(),
                provider,
                source.getWeight() == null ? 1.0 : source.getWeight(),
                source.getAuthorityScore() == null ? 0.7 : source.getAuthorityScore(),
                config
        );
    }

    private List<SourceRuntime> defaultSources() {
        return List.of(
                new SourceRuntime("腾讯新闻", "news", "tencent_news", 1.15, 0.86, Map.of()),
                new SourceRuntime("Google News RSS", "news", "google_news_rss", 0.85, 0.78, Map.of()),
                new SourceRuntime("Pexels", "image", "pexels", 0.95, 0.76, Map.of()),
                new SourceRuntime("Bilibili", "video", "bilibili", 1.0, 0.8, Map.of())
        );
    }

    private List<SearchItemDto> applySourceConfig(List<SearchItemDto> items, SourceRuntime source) {
        return items.stream().peek(item -> {
            if (StringUtils.hasText(source.name())) {
                item.setSourceName(source.name());
            }
            item.setAuthorityScore(source.authorityScore());
            item.setScore(source.weight());
            item.setTags(item.getTags() + "," + source.provider());
        }).toList();
    }

    private Map<String, String> parseConfig(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            Map<String, String> config = new LinkedHashMap<>();
            root.fields().forEachRemaining(entry -> config.put(entry.getKey(), entry.getValue().asText("")));
            return config;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String providerFromName(SearchSource source) {
        String name = source.getName() == null ? "" : source.getName().toLowerCase();
        if (name.contains("tencent") || name.contains("腾讯")) {
            return "tencent_news";
        }
        if (name.contains("google")) {
            return "google_news_rss";
        }
        if (name.contains("pexels")) {
            return "pexels";
        }
        if (name.contains("bilibili") || name.contains("b站")) {
            return "bilibili";
        }
        if (name.contains("serpapi") || name.contains("youtube")) {
            return "serpapi_youtube";
        }
        return switch (source.getType()) {
            case "image" -> "pexels";
            case "video" -> "bilibili";
            default -> "tencent_news";
        };
    }

    private record SourceRuntime(String name, String type, String provider, double weight, double authorityScore,
                                 Map<String, String> config) {
        String apiKey(String fallback) {
            return StringUtils.hasText(config.get("apiKey")) ? config.get("apiKey") : fallback;
        }
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
                item.setSummary(textOrDefault(cleanHtml(node.path("abstract").asText("")), "腾讯新闻搜索结果"));
                JsonNode thumbnails = node.path("thumbnails_qqnews");
                item.setThumbnailUrl(mediaProxyUrl(thumbnails.isArray() && !thumbnails.isEmpty() ? thumbnails.get(0).asText("") : ""));
                item.setPublishedAt(parseTencentTime(node));
                items.add(item);
            }
        }
        return items.stream().limit(24).toList();
    }

    public List<SearchItemDto> parseTencentHotNews(String json, String keyword) throws Exception {
        JsonNode articles = objectMapper.readTree(json).path("data").path("article_info").path("articles");
        List<SearchItemDto> items = new ArrayList<>();
        if (!articles.isArray()) {
            return items;
        }
        for (JsonNode node : articles) {
            addTencentHotNode(items, node, keyword);
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
            item.setSummary(textOrDefault(description, "Google 新闻搜索结果"));
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
            String url = firstText(node, "arcurl", "url");
            String thumbnailUrl = normalizeUrl(firstNonBlank(
                    node.path("pic").asText(""),
                    node.path("cover").asText(""),
                    node.path("pic_url").asText("")
            ));
            if (!StringUtils.hasText(title) || !StringUtils.hasText(url)
                    || isPaidBilibiliResult(node) || !isSupportedBilibiliThumbnail(thumbnailUrl)) {
                continue;
            }
            SearchItemDto item = baseItem("video", textOrDefault(node.path("author").asText(""), "B站"), keyword, title, items.size() + 1);
            item.setUrl(url);
            item.setSummary(textOrDefault(cleanHtml(node.path("description").asText("")), "B站视频搜索结果"));
            item.setThumbnailUrl(mediaProxyUrl(thumbnailUrl));
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
            item.setSummary(textOrDefault(node.path("snippet").asText(""), "YouTube 视频搜索结果"));
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
        item.setTags(sourceName);
        item.setAuthorityScore(0.82);
        return item;
    }

    private record TencentNewsPage(List<SearchItemDto> items, String searchId, boolean hasMore) {
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

    private void addTencentHotNode(List<SearchItemDto> items, JsonNode node, String keyword) {
        String title = textOrDefault(firstText(node, "short_title", "title"), node.path("share_info").path("share_title").asText(""));
        String url = firstText(node.path("link_info"), "url", "share_url", "short_url", "org_url");
        if (StringUtils.hasText(title) && StringUtils.hasText(url)) {
            SearchItemDto item = baseItem("news", tencentHotSourceName(node), keyword, cleanHtml(title), items.size() + 1);
            item.setUrl(url);
            item.setSummary(textOrDefault(cleanHtml(node.path("abstract").asText("")), "腾讯新闻热点"));
            item.setThumbnailUrl(mediaProxyUrl(tencentHotThumbnail(node)));
            item.setPublishedAt(parseTencentHotTime(node));
            items.add(item);
        }
        JsonNode subItems = node.path("sub_item");
        if (subItems.isArray()) {
            for (JsonNode subItem : subItems) {
                addTencentHotNode(items, subItem, keyword);
            }
        }
    }

    private String tencentHotSourceName(JsonNode node) {
        return textOrDefault(
                firstNonBlank(
                        node.path("media_info").path("chl_name").asText(""),
                        node.path("user_info").path("nick").asText("")
                ),
                "腾讯新闻"
        );
    }

    private String tencentHotThumbnail(JsonNode node) {
        JsonNode picInfo = node.path("pic_info");
        return firstNonBlank(
                firstArrayText(picInfo.path("small_img")),
                firstArrayText(picInfo.path("big_img")),
                firstArrayText(picInfo.path("three_img")),
                picInfo.path("share_img").asText(""),
                picInfo.path("ext").path("360x240").asText(""),
                picInfo.path("ext").path("196x130").asText("")
        );
    }

    private LocalDateTime parseTencentHotTime(JsonNode node) {
        String value = firstText(node, "publish_time", "update_time");
        if (StringUtils.hasText(value)) {
            try {
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ignored) {
            }
        }
        return LocalDateTime.now();
    }

    private String firstArrayText(JsonNode node) {
        return node.isArray() && !node.isEmpty() ? node.get(0).asText("") : "";
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

    private URI buildBilibiliVideoSearchUri(String keyword, int page, int size) throws Exception {
        JsonNode nav = objectMapper.readTree(restClient.get()
                .uri("https://api.bilibili.com/x/web-interface/nav")
                .retrieve()
                .body(String.class));
        String imgKey = fileKey(nav.path("data").path("wbi_img").path("img_url").asText(""));
        String subKey = fileKey(nav.path("data").path("wbi_img").path("sub_url").asText(""));
        String mixinKey = mixinKey(imgKey + subKey);
        Map<String, String> params = new TreeMap<>();
        params.put("keyword", query(keyword));
        params.put("page", String.valueOf(page));
        params.put("page_size", String.valueOf(size));
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

    private boolean isPaidBilibiliResult(JsonNode node) {
        String resultType = node.path("type").asText("");
        String url = firstText(node, "arcurl", "url");
        String badges = (node.path("badge").asText("") + " " + node.path("display_info").toString()).toLowerCase();
        return !"video".equalsIgnoreCase(resultType)
                || node.path("is_pay").asInt(0) > 0
                || node.path("badgepay").asBoolean(false)
                || url.contains("/cheese/")
                || badges.contains("付费")
                || badges.contains("pay");
    }

    private boolean isSupportedBilibiliThumbnail(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        try {
            URI uri = URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && (uri.getHost().endsWith(".hdslb.com") || "archive.biliimg.com".equalsIgnoreCase(uri.getHost()));
        } catch (Exception ignored) {
            return false;
        }
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

    private boolean isHotKeyword(String keyword) {
        return query(keyword).contains("热点");
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
