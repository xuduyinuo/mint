package com.mint.search.behavior;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mint.search.behavior.mapper.UserBehaviorMapper;
import com.mint.search.profile.UserProfile;
import com.mint.search.profile.mapper.UserProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class BehaviorService {
    private final UserBehaviorMapper behaviorMapper;
    private final UserProfileMapper profileMapper;

    public BehaviorService(UserBehaviorMapper behaviorMapper, UserProfileMapper profileMapper) {
        this.behaviorMapper = behaviorMapper;
        this.profileMapper = profileMapper;
    }

    public void record(Long userId, BehaviorRequest request) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setEventType(request.getEventType());
        behavior.setKeyword(request.getKeyword());
        behavior.setItemId(request.getItemId());
        behavior.setItemType(request.getItemType());
        behavior.setTags(request.getTags());
        behavior.setDurationSeconds(request.getDurationSeconds());
        behaviorMapper.insert(behavior);
        refreshProfile(userId, request);
    }

    private void refreshProfile(Long userId, BehaviorRequest request) {
        UserProfile profile = profileMapper.selectOne(new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId).last("limit 1"));
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setInterestTags("");
            profile.setPreferredTypes("");
        }
        boolean highIntent = "CLICK".equalsIgnoreCase(request.getEventType());
        profile.setInterestTags(merge(profile.getInterestTags(), highIntent, request.getKeyword(), request.getTags()));
        profile.setPreferredTypes(merge(profile.getPreferredTypes(), highIntent, request.getItemType()));
        if (profile.getId() == null) {
            profileMapper.insert(profile);
        } else {
            profileMapper.updateById(profile);
        }
    }

    private String merge(String current, boolean additionsFirst, String... additions) {
        Set<String> values = new LinkedHashSet<>();
        if (additionsFirst) {
            add(values, additions);
            add(values, current);
        } else {
            add(values, current);
            add(values, additions);
        }
        return String.join(",", values.stream().limit(12).toList());
    }

    private void add(Set<String> values, String... additions) {
        for (String addition : additions) {
            if (StringUtils.hasText(addition)) {
                for (String value : addition.split(",")) {
                    if (StringUtils.hasText(value)) values.add(value.trim());
                }
            }
        }
    }
}
