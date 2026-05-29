package com.mint.search.core;

import com.mint.search.search.dto.SearchItemDto;
import com.mint.search.search.service.MockSearchProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockSearchProviderTest {

    @Test
    void returnsSupportedTypesWhenTypeIsAll() {
        MockSearchProvider provider = new MockSearchProvider();

        List<SearchItemDto> items = provider.search("智能推荐", "all");

        assertThat(items).hasSizeGreaterThanOrEqualTo(6);
        assertThat(items).extracting(SearchItemDto::getType)
                .contains("news", "image", "video")
                .doesNotContain("web");
        assertThat(items).allSatisfy(item -> assertThat(item.getTitle()).contains("智能推荐"));
    }

    @Test
    void filtersSpecificType() {
        MockSearchProvider provider = new MockSearchProvider();

        List<SearchItemDto> items = provider.search("AI", "video");

        assertThat(items).isNotEmpty();
        assertThat(items).allSatisfy(item -> assertThat(item.getType()).isEqualTo("video"));
    }
}
