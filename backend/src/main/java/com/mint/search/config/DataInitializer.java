package com.mint.search.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mint.search.hot.mapper.HotRecommendationMapper;
import com.mint.search.source.SearchSource;
import com.mint.search.source.mapper.SearchSourceMapper;
import com.mint.search.user.SysUser;
import com.mint.search.user.mapper.SysUserMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final SysUserMapper userMapper;
    private final SearchSourceMapper sourceMapper;
    private final HotRecommendationMapper hotMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SysUserMapper userMapper, SearchSourceMapper sourceMapper, HotRecommendationMapper hotMapper,
                           PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.sourceMapper = sourceMapper;
        this.hotMapper = hotMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createUser("admin", "admin123456", "系统管理员", "ADMIN");
        createUser("user", "user123456", "演示用户", "USER");
        createSource("NewAPI News", "news", 1.1, 0.86);
        createSource("NewAPI Image", "image", 0.9, 0.72);
        createSource("NewAPI Video", "video", 0.95, 0.78);
        deleteLegacyWebSources();
        clearDemoHotRecommendations();
    }

    private void createUser(String username, String password, String nickname, String role) {
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) > 0) {
            return;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setRole(role);
        user.setStatus(1);
        userMapper.insert(user);
    }

    private void createSource(String name, String type, double weight, double authority) {
        if (sourceMapper.selectCount(new LambdaQueryWrapper<SearchSource>().eq(SearchSource::getName, name)) > 0) {
            return;
        }
        SearchSource source = new SearchSource();
        source.setName(name);
        source.setType(type);
        source.setEnabled(1);
        source.setWeight(weight);
        source.setAuthorityScore(authority);
        source.setConfigJson("{}");
        sourceMapper.insert(source);
    }

    private void deleteLegacyWebSources() {
        sourceMapper.delete(new LambdaQueryWrapper<SearchSource>().eq(SearchSource::getType, "web"));
    }

    private void clearDemoHotRecommendations() {
        hotMapper.delete(new LambdaQueryWrapper<com.mint.search.hot.HotRecommendation>()
                .like(com.mint.search.hot.HotRecommendation::getUrl, "example.com")
                .or()
                .like(com.mint.search.hot.HotRecommendation::getThumbnailUrl, "picsum.photos"));
    }
}
