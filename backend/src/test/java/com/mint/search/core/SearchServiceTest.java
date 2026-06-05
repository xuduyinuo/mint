package com.mint.search.core;

import com.mint.search.blog.mapper.BlogPostMapper;
import com.mint.search.log.mapper.SearchLogMapper;
import com.mint.search.profile.mapper.UserProfileMapper;
import com.mint.search.search.dto.SearchItemDto;
import com.mint.search.search.service.DomesticSearchProvider;
import com.mint.search.search.service.RankingService;
import com.mint.search.search.service.SearchService;
import com.mint.search.upload.mapper.UploadedAssetMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchServiceTest {
    @Test
    void imageSearchKeepsProviderImagesWhenMetadataUsesAnotherLanguage() {
        DomesticSearchProvider provider = mock(DomesticSearchProvider.class);
        SearchItemDto image = item("image", "A red car in city traffic", "Pexels");
        when(provider.search(eq("汽车"), eq("image"), anyInt(), anyInt())).thenReturn(List.of(image));

        SearchService service = service(provider);

        var response = service.search("汽车", "image", 1, 12, null);

        assertThat(response.getRecords()).hasSize(1);
        assertThat(response.getRecords().getFirst().getTitle()).isEqualTo("A red car in city traffic");
    }

    @Test
    void allSearchStillFiltersProviderImagesThatOnlyMatchBySourceTrust() {
        DomesticSearchProvider provider = mock(DomesticSearchProvider.class);
        SearchItemDto image = item("image", "A red car in city traffic", "Pexels");
        when(provider.search(eq("汽车"), eq("all"), anyInt(), anyInt())).thenReturn(List.of(image));

        SearchService service = service(provider);

        var response = service.search("汽车", "all", 1, 12, null);

        assertThat(response.getRecords()).isEmpty();
    }

    private SearchService service(DomesticSearchProvider provider) {
        return new SearchService(
                provider,
                new RankingService(),
                mock(SearchLogMapper.class),
                mock(UserProfileMapper.class),
                mock(BlogPostMapper.class),
                mock(UploadedAssetMapper.class)
        );
    }

    private SearchItemDto item(String type, String title, String sourceName) {
        SearchItemDto item = new SearchItemDto();
        item.setExternalId(sourceName + "-" + title.hashCode());
        item.setType(type);
        item.setTitle(title);
        item.setSourceName(sourceName);
        item.setSummary("Pexels 图片，摄影师：Alice");
        item.setTags("Pexels,pexels");
        item.setAuthorityScore(0.7);
        return item;
    }
}
