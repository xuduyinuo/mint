package com.mint.search.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mint.search.behavior.mapper.UserBehaviorMapper;
import com.mint.search.hot.mapper.HotRecommendationMapper;
import com.mint.search.log.mapper.SearchLogMapper;
import com.mint.search.source.mapper.SearchSourceMapper;
import com.mint.search.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminService {
    private final SysUserMapper userMapper;
    private final SearchSourceMapper sourceMapper;
    private final HotRecommendationMapper hotMapper;
    private final SearchLogMapper logMapper;
    private final UserBehaviorMapper behaviorMapper;

    public AdminService(SysUserMapper userMapper, SearchSourceMapper sourceMapper, HotRecommendationMapper hotMapper,
                        SearchLogMapper logMapper, UserBehaviorMapper behaviorMapper) {
        this.userMapper = userMapper;
        this.sourceMapper = sourceMapper;
        this.hotMapper = hotMapper;
        this.logMapper = logMapper;
        this.behaviorMapper = behaviorMapper;
    }

    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("userCount", userMapper.selectCount(null));
        stats.put("sourceCount", sourceMapper.selectCount(null));
        stats.put("hotCount", hotMapper.selectCount(null));
        stats.put("searchCount", logMapper.selectCount(null));
        stats.put("behaviorCount", behaviorMapper.selectCount(null));
        stats.put("enabledSources", sourceMapper.selectCount(new LambdaQueryWrapper<com.mint.search.source.SearchSource>().eq(com.mint.search.source.SearchSource::getEnabled, 1)));
        return stats;
    }
}
