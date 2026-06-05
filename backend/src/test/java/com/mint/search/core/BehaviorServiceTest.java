package com.mint.search.core;

import com.mint.search.behavior.BehaviorRequest;
import com.mint.search.behavior.BehaviorService;
import com.mint.search.behavior.mapper.UserBehaviorMapper;
import com.mint.search.profile.UserProfile;
import com.mint.search.profile.mapper.UserProfileMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BehaviorServiceTest {
    @Test
    void clickBehaviorMovesRecentInterestsAndTypesToTheFront() {
        UserProfile existing = new UserProfile();
        existing.setId(1L);
        existing.setUserId(7L);
        existing.setInterestTags("AI,新闻");
        existing.setPreferredTypes("news");
        UserProfileMapper profileMapper = mock(UserProfileMapper.class);
        when(profileMapper.selectOne(any())).thenReturn(existing);

        BehaviorService service = new BehaviorService(mock(UserBehaviorMapper.class), profileMapper);
        BehaviorRequest request = new BehaviorRequest();
        request.setEventType("CLICK");
        request.setKeyword("摄影");
        request.setItemType("image");
        request.setTags("城市,AI");

        service.record(7L, request);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileMapper).updateById(captor.capture());
        assertThat(captor.getValue().getInterestTags()).isEqualTo("摄影,城市,AI,新闻");
        assertThat(captor.getValue().getPreferredTypes()).isEqualTo("image,news");
    }
}
