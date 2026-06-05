package com.mint.search.recommendation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mint.search.blog.BlogPost;
import com.mint.search.blog.mapper.BlogPostMapper;
import com.mint.search.hot.HotDataService;
import com.mint.search.profile.UserProfile;
import com.mint.search.profile.mapper.UserProfileMapper;
import com.mint.search.search.dto.SearchItemDto;
import com.mint.search.search.service.RankingService;
import com.mint.search.upload.UploadedAsset;
import com.mint.search.upload.mapper.UploadedAssetMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    private static final List<String> SUPPORTED_TYPES = List.of("all", "news", "image", "video", "blog");

    private final UserProfileMapper profileMapper;
    private final HotDataService hotDataService;
    private final RankingService rankingService;
    private final BlogPostMapper blogPostMapper;
    private final UploadedAssetMapper assetMapper;

    public RecommendationService(UserProfileMapper profileMapper, HotDataService hotDataService,
                                 RankingService rankingService, BlogPostMapper blogPostMapper,
                                 UploadedAssetMapper assetMapper) {
        this.profileMapper = profileMapper;
        this.hotDataService = hotDataService;
        this.rankingService = rankingService;
        this.blogPostMapper = blogPostMapper;
        this.assetMapper = assetMapper;
    }

    public List<SearchItemDto> recommend(Long userId, String type) {
        String recommendType = normalizeType(type);
        if ("blog".equals(recommendType)) {
            return blogRecommendations();
        }
        if (userId == null) {
            return hotDataService.getHotData(recommendType).stream().limit(12).toList();
        }
        UserProfile profile = profile(userId);
        List<SearchItemDto> ranked = rankingService.rank("今日热点", recommendType, typePreferences(profile), candidates(recommendType));
        return ranked.stream()
                .sorted(Comparator.comparing((SearchItemDto item) -> personalizedScore(item, profile)).reversed())
                .limit(12)
                .toList();
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "all";
        }
        String normalized = type.trim().toLowerCase();
        return SUPPORTED_TYPES.contains(normalized) ? normalized : "all";
    }

    private List<SearchItemDto> blogRecommendations() {
        if (blogPostMapper == null) {
            return List.of();
        }
        return blogPostMapper.selectList(new LambdaQueryWrapper<BlogPost>()
                        .eq(BlogPost::getStatus, "PUBLISHED")
                        .eq(BlogPost::getBlocked, 0)
                        .orderByDesc(BlogPost::getUpdateTime))
                .stream()
                .limit(12)
                .map(this::blogItem)
                .toList();
    }

    private UserProfile profile(Long userId) {
        if (profileMapper == null || userId == null) {
            return null;
        }
        return profileMapper.selectOne(new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId).last("limit 1"));
    }

    private List<SearchItemDto> candidates(String type) {
        Map<String, SearchItemDto> items = new LinkedHashMap<>();
        hotDataService.getHotData(type).forEach(item -> items.put(key(item), item));
        if ("all".equals(type) || "blog".equals(type)) {
            blogRecommendations().forEach(item -> items.put(key(item), item));
        }
        if ("all".equals(type) || "image".equals(type)) {
            imageRecommendations().forEach(item -> items.put(key(item), item));
        }
        return new ArrayList<>(items.values());
    }

    private List<SearchItemDto> imageRecommendations() {
        if (assetMapper == null) {
            return List.of();
        }
        return assetMapper.selectList(new LambdaQueryWrapper<UploadedAsset>()
                        .eq(UploadedAsset::getAssetType, "image")
                        .eq(UploadedAsset::getBlocked, 0)
                        .orderByDesc(UploadedAsset::getCreateTime))
                .stream()
                .limit(12)
                .map(this::imageItem)
                .toList();
    }

    private Map<String, Double> typePreferences(UserProfile profile) {
        if (profile == null || !StringUtils.hasText(profile.getPreferredTypes())) {
            return Map.of();
        }
        Map<String, Double> prefs = new LinkedHashMap<>();
        int index = 0;
        for (String type : profile.getPreferredTypes().split(",")) {
            if (StringUtils.hasText(type)) {
                prefs.put(type.trim(), Math.max(1.15, 1.9 - index * 0.15));
                index++;
            }
        }
        return prefs;
    }

    private double personalizedScore(SearchItemDto item, UserProfile profile) {
        double score = item.getScore() == null ? 0.0 : item.getScore();
        if (profile == null) {
            return score;
        }
        score += typeBoost(item, profile);
        score += tagBoost(item, profile);
        return score;
    }

    private double typeBoost(SearchItemDto item, UserProfile profile) {
        if (!StringUtils.hasText(item.getType()) || !StringUtils.hasText(profile.getPreferredTypes())) {
            return 0.0;
        }
        List<String> types = split(profile.getPreferredTypes());
        int index = types.indexOf(item.getType());
        return index < 0 ? 0.0 : Math.max(0.2, 1.2 - index * 0.15);
    }

    private double tagBoost(SearchItemDto item, UserProfile profile) {
        Set<String> interests = split(profile.getInterestTags()).stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        if (interests.isEmpty()) {
            return 0.0;
        }
        String text = ((item.getTitle() == null ? "" : item.getTitle()) + "," +
                (item.getSummary() == null ? "" : item.getSummary()) + "," +
                (item.getTags() == null ? "" : item.getTags())).toLowerCase();
        return interests.stream().filter(text::contains).count() * 0.55;
    }

    private List<String> split(String values) {
        if (!StringUtils.hasText(values)) {
            return List.of();
        }
        return List.of(values.split(",")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
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

    private String key(SearchItemDto item) {
        if (StringUtils.hasText(item.getExternalId())) {
            return item.getExternalId();
        }
        return item.getType() + ":" + item.getTitle() + ":" + item.getUrl();
    }

    private String excerpt(String content) {
        if (!StringUtils.hasText(content)) {
            return "平台原创博客";
        }
        String plain = content.replaceAll("[#*_`>\\[\\]()!]", " ").replaceAll("\\s+", " ").trim();
        return plain.length() > 160 ? plain.substring(0, 160) + "..." : plain;
    }
}
