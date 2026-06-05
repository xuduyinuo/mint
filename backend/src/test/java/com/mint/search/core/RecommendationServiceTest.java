package com.mint.search.core;

import com.mint.search.hot.HotDataService;
import com.mint.search.profile.UserProfile;
import com.mint.search.profile.mapper.UserProfileMapper;
import com.mint.search.recommendation.RecommendationService;
import com.mint.search.search.dto.SearchItemDto;
import com.mint.search.search.service.RankingService;
import com.mint.search.upload.mapper.UploadedAssetMapper;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendationServiceTest {
    @Test
    void anonymousRecommendationsUseHotData() {
        SearchItemDto item = new SearchItemDto();
        item.setTitle("热点新闻");
        item.setType("news");
        item.setSourceName("热点缓存");
        item.setUrl("https://example.com/hot");
        HotDataService hotDataService = mock(HotDataService.class);
        when(hotDataService.getHotData("all")).thenReturn(List.of(item));

        RecommendationService service = new RecommendationService(
                null,
                hotDataService,
                new RankingService(),
                null,
                null
        );

        var recommendations = service.recommend(null, "all");

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.getFirst().getSourceName()).isEqualTo("热点缓存");
    }

    @Test
    void returnsEmptyWhenHotDataIsEmpty() {
        HotDataService hotDataService = mock(HotDataService.class);
        when(hotDataService.getHotData("all")).thenReturn(List.of());

        RecommendationService service = new RecommendationService(
                null,
                hotDataService,
                new RankingService(),
                null,
                null
        );

        assertThat(service.recommend(null, "all")).isEmpty();
    }

    @Test
    void blogRecommendationsDoNotFallBackToNews() {
        SearchItemDto item = new SearchItemDto();
        item.setTitle("热点新闻");
        item.setType("news");
        HotDataService hotDataService = mock(HotDataService.class);
        when(hotDataService.getHotData("all")).thenReturn(List.of(item));

        RecommendationService service = new RecommendationService(
                null,
                hotDataService,
                new RankingService(),
                null,
                null
        );

        assertThat(service.recommend(null, "blog")).isEmpty();
    }

    @Test
    void loggedInUsersReceiveDifferentOrderingFromTheSameCandidates() {
        SearchItemDto news = item("news-1", "AI 大模型新闻", "news", "AI,模型");
        SearchItemDto image = item("image-1", "城市摄影作品", "image", "摄影,城市");
        HotDataService hotDataService = mock(HotDataService.class);
        when(hotDataService.getHotData("all")).thenReturn(List.of(news, image));

        UserProfileMapper profileMapper = mock(UserProfileMapper.class);
        UserProfile newsProfile = profile("AI,模型", "news");
        UserProfile imageProfile = profile("摄影,城市", "image");
        when(profileMapper.selectOne(any())).thenReturn(newsProfile, imageProfile);

        RecommendationService service = new RecommendationService(
                profileMapper,
                hotDataService,
                new RankingService(),
                null,
                mock(UploadedAssetMapper.class)
        );

        var newsRecommendations = service.recommend(10L, "all");
        var imageRecommendations = service.recommend(20L, "all");

        assertThat(newsRecommendations).extracting(SearchItemDto::getExternalId)
                .containsExactly("news-1", "image-1");
        assertThat(imageRecommendations).extracting(SearchItemDto::getExternalId)
                .containsExactly("image-1", "news-1");
    }

    private SearchItemDto item(String id, String title, String type, String tags) {
        SearchItemDto item = new SearchItemDto();
        item.setExternalId(id);
        item.setTitle(title);
        item.setType(type);
        item.setTags(tags);
        item.setSourceName("候选池");
        item.setSummary(title);
        item.setAuthorityScore(0.7);
        return item;
    }

    private UserProfile profile(String tags, String types) {
        UserProfile profile = new UserProfile();
        profile.setInterestTags(tags);
        profile.setPreferredTypes(types);
        return profile;
    }
}
