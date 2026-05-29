package com.mint.search.recommendation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mint.search.hot.HotDataService;
import com.mint.search.profile.UserProfile;
import com.mint.search.profile.mapper.UserProfileMapper;
import com.mint.search.search.dto.SearchItemDto;
import com.mint.search.search.service.RankingService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {
    private static final List<String> SUPPORTED_TYPES = List.of("all", "news", "image", "video");

    private final UserProfileMapper profileMapper;
    private final HotDataService hotDataService;
    private final RankingService rankingService;

    public RecommendationService(UserProfileMapper profileMapper, HotDataService hotDataService,
                                 RankingService rankingService) {
        this.profileMapper = profileMapper;
        this.hotDataService = hotDataService;
        this.rankingService = rankingService;
    }

    public List<SearchItemDto> recommend(Long userId, String type) {
        String recommendType = normalizeType(type);
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
}
