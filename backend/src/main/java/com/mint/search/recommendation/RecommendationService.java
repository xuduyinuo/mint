package com.mint.search.recommendation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mint.search.blog.BlogPost;
import com.mint.search.blog.mapper.BlogPostMapper;
import com.mint.search.hot.HotDataService;
import com.mint.search.profile.UserProfile;
import com.mint.search.profile.mapper.UserProfileMapper;
import com.mint.search.search.dto.SearchItemDto;
import com.mint.search.search.service.RankingService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {
    private static final List<String> SUPPORTED_TYPES = List.of("all", "news", "image", "video", "blog");

    private final UserProfileMapper profileMapper;
    private final HotDataService hotDataService;
    private final RankingService rankingService;
    private final BlogPostMapper blogPostMapper;

    public RecommendationService(UserProfileMapper profileMapper, HotDataService hotDataService,
                                 RankingService rankingService, BlogPostMapper blogPostMapper) {
        this.profileMapper = profileMapper;
        this.hotDataService = hotDataService;
        this.rankingService = rankingService;
        this.blogPostMapper = blogPostMapper;
    }

    public List<SearchItemDto> recommend(Long userId, String type) {
        String recommendType = normalizeType(type);
        if ("blog".equals(recommendType)) {
            return blogRecommendations();
        }
        if (userId == null) {
            return hotDataService.getHotData(recommendType).stream().limit(12).toList();
        }
        UserProfile profile = profileMapper.selectOne(new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId).last("limit 1"));
        Map<String, Double> prefs = profile == null || profile.getPreferredTypes() == null || profile.getPreferredTypes().isBlank()
                ? Map.of()
                : Map.of(profile.getPreferredTypes().split(",")[0], 1.8);
        return rankingService.rank("今日热点", recommendType, prefs, hotDataService.getHotData(recommendType)).stream().limit(12).toList();
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

    private String excerpt(String content) {
        if (!StringUtils.hasText(content)) {
            return "平台原创博客";
        }
        String plain = content.replaceAll("[#*_`>\\[\\]()!]", " ").replaceAll("\\s+", " ").trim();
        return plain.length() > 160 ? plain.substring(0, 160) + "..." : plain;
    }
}
