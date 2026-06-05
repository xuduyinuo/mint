package com.mint.search.search.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mint.search.blog.BlogPost;
import com.mint.search.blog.mapper.BlogPostMapper;
import com.mint.search.log.SearchLog;
import com.mint.search.log.mapper.SearchLogMapper;
import com.mint.search.profile.UserProfile;
import com.mint.search.profile.mapper.UserProfileMapper;
import com.mint.search.search.dto.SearchItemDto;
import com.mint.search.search.dto.SearchResponse;
import com.mint.search.upload.UploadedAsset;
import com.mint.search.upload.mapper.UploadedAssetMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {
    private static final List<String> SUPPORTED_TYPES = List.of("all", "news", "image", "video", "blog");

    private final DomesticSearchProvider domesticSearchProvider;
    private final RankingService rankingService;
    private final SearchLogMapper searchLogMapper;
    private final UserProfileMapper profileMapper;
    private final BlogPostMapper blogPostMapper;
    private final UploadedAssetMapper assetMapper;

    public SearchService(DomesticSearchProvider domesticSearchProvider, RankingService rankingService,
                         SearchLogMapper searchLogMapper, UserProfileMapper profileMapper,
                         BlogPostMapper blogPostMapper, UploadedAssetMapper assetMapper) {
        this.domesticSearchProvider = domesticSearchProvider;
        this.rankingService = rankingService;
        this.searchLogMapper = searchLogMapper;
        this.profileMapper = profileMapper;
        this.blogPostMapper = blogPostMapper;
        this.assetMapper = assetMapper;
    }

    public SearchResponse search(String keyword, String type, int page, int size, Long userId) {
        long start = System.currentTimeMillis();
        String searchType = normalizeType(type);
        List<SearchItemDto> items = new ArrayList<>(domesticSearchProvider.search(keyword, searchType, page, size));
        items.addAll(originalContent(keyword, searchType));
        items = filterRelevant(keyword, searchType, items);
        List<SearchItemDto> ranked = rankingService.rank(keyword, searchType, preferences(userId), items);
        List<SearchItemDto> pageItems = ranked.stream().limit(size).toList();
        boolean hasNext = ranked.size() >= size;
        long estimatedTotal = (long) (page - 1) * size + pageItems.size() + (hasNext ? size : 0);
        Map<String, Long> distribution = ranked.stream()
                .collect(Collectors.groupingBy(SearchItemDto::getSourceName, LinkedHashMap::new, Collectors.counting()));
        saveLog(userId, keyword, searchType, pageItems.size(), System.currentTimeMillis() - start, distribution);
        return new SearchResponse(keyword, searchType, pageItems, estimatedTotal, page, size, hasNext,
                "score = (相关性 + 时效性 + 权威性 + 用户偏好 + 类型匹配) × 搜索源权重", distribution);
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "all";
        }
        String normalized = type.trim().toLowerCase();
        return SUPPORTED_TYPES.contains(normalized) ? normalized : "all";
    }

    private List<SearchItemDto> originalContent(String keyword, String type) {
        List<SearchItemDto> items = new ArrayList<>();
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : "";
        if ("all".equals(type) || "blog".equals(type)) {
            blogPostMapper.selectList(new LambdaQueryWrapper<BlogPost>()
                            .eq(BlogPost::getStatus, "PUBLISHED")
                            .eq(BlogPost::getBlocked, 0)
                            .orderByDesc(BlogPost::getUpdateTime))
                    .stream()
                    .filter(post -> matches(normalizedKeyword, post.getTitle(), post.getSummary(), post.getTags(), post.getContent()))
                    .map(this::blogItem)
                    .forEach(items::add);
        }
        if ("all".equals(type) || "image".equals(type)) {
            assetMapper.selectList(new LambdaQueryWrapper<UploadedAsset>()
                            .eq(UploadedAsset::getAssetType, "image")
                            .eq(UploadedAsset::getBlocked, 0)
                            .orderByDesc(UploadedAsset::getCreateTime))
                    .stream()
                    .filter(asset -> matches(normalizedKeyword, asset.getFileName(), asset.getTags()))
                    .map(this::imageItem)
                    .forEach(items::add);
        }
        return items;
    }

    private boolean matches(String keyword, String... values) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        for (String value : values) {
            if (StringUtils.hasText(value) && value.toLowerCase().contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<SearchItemDto> filterRelevant(String keyword, String type, List<SearchItemDto> items) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : "";
        if (!StringUtils.hasText(normalizedKeyword)) {
            return items;
        }
        List<String> tokens = List.of(normalizedKeyword.split("\\s+")).stream()
                .filter(StringUtils::hasText)
                .toList();
        if (tokens.isEmpty()) {
            return items;
        }
        return items.stream()
                .filter(item -> isTrustedTypedImage(type, item)
                        || tokens.stream().anyMatch(token -> matches(token, item.getTitle(), item.getSummary(), item.getTags())))
                .toList();
    }

    private boolean isTrustedTypedImage(String type, SearchItemDto item) {
        return "image".equals(type)
                && "image".equals(item.getType())
                && !"Mint 原创图片".equals(item.getSourceName());
    }

    private SearchItemDto blogItem(BlogPost post) {
        SearchItemDto item = new SearchItemDto();
        item.setExternalId("mint-blog-" + post.getId());
        item.setType("blog");
        item.setSourceName("Mint 原创博客");
        item.setTitle(post.getTitle());
        item.setSummary(StringUtils.hasText(post.getSummary()) ? post.getSummary() : excerpt(post.getContent()));
        item.setUrl("/creator/blogs");
        item.setThumbnailUrl(post.getCoverUrl());
        item.setTags(post.getTags());
        item.setAuthorityScore(0.9);
        item.setPublishedAt(post.getUpdateTime());
        return item;
    }

    private SearchItemDto imageItem(UploadedAsset asset) {
        SearchItemDto item = new SearchItemDto();
        item.setExternalId("mint-image-" + asset.getId());
        item.setType("image");
        item.setSourceName("Mint 原创图片");
        item.setTitle(asset.getFileName());
        item.setSummary(StringUtils.hasText(asset.getTags()) ? "标签：" + asset.getTags() : "平台原创图片");
        item.setUrl(asset.getUrl());
        item.setThumbnailUrl(asset.getUrl());
        item.setTags(asset.getTags());
        item.setAuthorityScore(0.86);
        item.setPublishedAt(asset.getCreateTime());
        return item;
    }

    private String excerpt(String content) {
        if (!StringUtils.hasText(content)) {
            return "平台原创博客";
        }
        String plain = content.replaceAll("[#*_`>\\[\\]()!]", " ").replaceAll("\\s+", " ").trim();
        return plain.length() > 160 ? plain.substring(0, 160) + "..." : plain;
    }

    private Map<String, Double> preferences(Long userId) {
        if (userId == null) {
            return Map.of();
        }
        UserProfile profile = profileMapper.selectOne(new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId).last("limit 1"));
        if (profile == null || profile.getPreferredTypes() == null) {
            return Map.of();
        }
        Map<String, Double> prefs = new LinkedHashMap<>();
        for (String type : profile.getPreferredTypes().split(",")) {
            if (!type.isBlank()) prefs.put(type.trim(), 1.8);
        }
        return prefs;
    }

    private void saveLog(Long userId, String keyword, String type, int resultCount, long durationMs, Map<String, Long> distribution) {
        SearchLog log = new SearchLog();
        log.setUserId(userId);
        log.setKeyword(keyword);
        log.setType(type);
        log.setResultCount(resultCount);
        log.setDurationMs(durationMs);
        log.setSourceDistribution(distribution.toString());
        searchLogMapper.insert(log);
    }
}
