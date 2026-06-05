package com.mint.search.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mint.search.hot.mapper.HotRecommendationMapper;
import com.mint.search.ranking.RankingConfig;
import com.mint.search.ranking.mapper.RankingConfigMapper;
import com.mint.search.source.SearchSource;
import com.mint.search.source.mapper.SearchSourceMapper;
import com.mint.search.user.SysUser;
import com.mint.search.user.mapper.SysUserMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final SysUserMapper userMapper;
    private final SearchSourceMapper sourceMapper;
    private final HotRecommendationMapper hotMapper;
    private final RankingConfigMapper rankingMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(SysUserMapper userMapper, SearchSourceMapper sourceMapper, HotRecommendationMapper hotMapper,
                           RankingConfigMapper rankingMapper, PasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate) {
        this.userMapper = userMapper;
        this.sourceMapper = sourceMapper;
        this.hotMapper = hotMapper;
        this.rankingMapper = rankingMapper;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        ensureContentColumns();
        createUser("admin", "admin123456", "系统管理员", "ADMIN");
        createUser("user", "user123456", "演示用户", "USER");
        createSource("腾讯新闻", "news", 1.15, 0.86, """
                {"provider":"tencent_news","endpoint":"https://i.news.qq.com/gw/pc_search/result","requiresApiKey":false,"notes":"新闻搜索主来源，今日热点会优先读取腾讯新闻热点榜。"}
                """);
        createSource("Google News RSS", "news", 0.85, 0.78, """
                {"provider":"google_news_rss","endpoint":"https://news.google.com/rss/search","requiresApiKey":false,"notes":"新闻搜索备用来源，腾讯新闻无结果时可单独启用补充。"}
                """);
        createSource("Pexels", "image", 0.95, 0.76, """
                {"provider":"pexels","endpoint":"https://api.pexels.com/v1/search","requiresApiKey":true,"apiKeyFrom":"PEXELS_API_KEY","notes":"图片搜索来源，可在配置 JSON 中填写 apiKey 覆盖环境变量。"}
                """);
        createSource("Bilibili", "video", 1.0, 0.8, """
                {"provider":"bilibili","endpoint":"https://api.bilibili.com/x/web-interface/search/type","requiresApiKey":false,"notes":"视频搜索主来源，会过滤付费课程等非普通视频结果。"}
                """);
        createSource("SerpAPI YouTube", "video", 0.75, 0.74, """
                {"provider":"serpapi_youtube","endpoint":"https://serpapi.com/search?engine=youtube","requiresApiKey":true,"apiKeyFrom":"SERPAPI_API_KEY","notes":"可选视频补充来源，填写 SerpAPI key 后启用。"}
                """, 0);
        createRankingConfig();
        deleteLegacyWebSources();
        disableLegacyNewApiSources();
        clearDemoHotRecommendations();
    }

    private void ensureContentColumns() {
        addColumnIfMissing("uploaded_asset", "tags", "VARCHAR(512)");
        addColumnIfMissing("uploaded_asset", "blocked", "TINYINT NOT NULL DEFAULT 0");
        addColumnIfMissing("blog_post", "tags", "VARCHAR(512)");
        addColumnIfMissing("blog_post", "blocked", "TINYINT NOT NULL DEFAULT 0");
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
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

    private void createSource(String name, String type, double weight, double authority, String configJson) {
        createSource(name, type, weight, authority, configJson, 1);
    }

    private void createSource(String name, String type, double weight, double authority, String configJson, int enabled) {
        if (sourceMapper.selectCount(new LambdaQueryWrapper<SearchSource>().eq(SearchSource::getName, name)) > 0) {
            return;
        }
        SearchSource source = new SearchSource();
        source.setName(name);
        source.setType(type);
        source.setEnabled(enabled);
        source.setWeight(weight);
        source.setAuthorityScore(authority);
        source.setConfigJson(configJson.trim());
        sourceMapper.insert(source);
    }

    private void createRankingConfig() {
        if (rankingMapper.selectCount(new LambdaQueryWrapper<RankingConfig>().eq(RankingConfig::getName, "默认综合排序")) > 0) {
            return;
        }
        RankingConfig config = new RankingConfig();
        config.setName("默认综合排序");
        config.setRelevanceWeight(0.45);
        config.setFreshnessWeight(0.2);
        config.setAuthorityWeight(0.25);
        config.setPreferenceWeight(0.1);
        config.setEnabled(1);
        rankingMapper.insert(config);
    }

    private void deleteLegacyWebSources() {
        sourceMapper.delete(new LambdaQueryWrapper<SearchSource>().eq(SearchSource::getType, "web"));
    }

    private void disableLegacyNewApiSources() {
        sourceMapper.selectList(new LambdaQueryWrapper<SearchSource>()
                .likeRight(SearchSource::getName, "NewAPI"))
                .forEach(source -> {
                    source.setEnabled(0);
                    source.setConfigJson("""
                            {"provider":"newapi","deprecated":true,"notes":"历史演示来源，当前主搜索链路未使用。"}
                            """.trim());
                    sourceMapper.updateById(source);
                });
    }

    private void clearDemoHotRecommendations() {
        hotMapper.delete(new LambdaQueryWrapper<com.mint.search.hot.HotRecommendation>()
                .like(com.mint.search.hot.HotRecommendation::getUrl, "example.com")
                .or()
                .like(com.mint.search.hot.HotRecommendation::getThumbnailUrl, "picsum.photos"));
    }
}
