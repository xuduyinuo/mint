package com.mint.search.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mint.search.admin.dto.AdminBlogContentDto;
import com.mint.search.admin.dto.AdminImageContentDto;
import com.mint.search.admin.dto.AdminUserDto;
import com.mint.search.admin.dto.AdminUserUpdateRequest;
import com.mint.search.behavior.UserBehavior;
import com.mint.search.behavior.mapper.UserBehaviorMapper;
import com.mint.search.blog.BlogPost;
import com.mint.search.blog.mapper.BlogPostMapper;
import com.mint.search.common.PageResult;
import com.mint.search.hot.HotRecommendation;
import com.mint.search.hot.mapper.HotRecommendationMapper;
import com.mint.search.log.SearchLog;
import com.mint.search.log.mapper.SearchLogMapper;
import com.mint.search.profile.UserProfile;
import com.mint.search.profile.mapper.UserProfileMapper;
import com.mint.search.ranking.RankingConfig;
import com.mint.search.ranking.mapper.RankingConfigMapper;
import com.mint.search.source.SearchSource;
import com.mint.search.source.mapper.SearchSourceMapper;
import com.mint.search.upload.UploadedAsset;
import com.mint.search.upload.mapper.UploadedAssetMapper;
import com.mint.search.user.SysUser;
import com.mint.search.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class AdminService {
    private final SysUserMapper userMapper;
    private final SearchSourceMapper sourceMapper;
    private final HotRecommendationMapper hotMapper;
    private final SearchLogMapper logMapper;
    private final UserBehaviorMapper behaviorMapper;
    private final UserProfileMapper profileMapper;
    private final RankingConfigMapper rankingMapper;
    private final BlogPostMapper blogMapper;
    private final UploadedAssetMapper assetMapper;

    public AdminService(SysUserMapper userMapper, SearchSourceMapper sourceMapper, HotRecommendationMapper hotMapper,
                        SearchLogMapper logMapper, UserBehaviorMapper behaviorMapper, UserProfileMapper profileMapper,
                        RankingConfigMapper rankingMapper, BlogPostMapper blogMapper, UploadedAssetMapper assetMapper) {
        this.userMapper = userMapper;
        this.sourceMapper = sourceMapper;
        this.hotMapper = hotMapper;
        this.logMapper = logMapper;
        this.behaviorMapper = behaviorMapper;
        this.profileMapper = profileMapper;
        this.rankingMapper = rankingMapper;
        this.blogMapper = blogMapper;
        this.assetMapper = assetMapper;
    }

    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("userCount", userMapper.selectCount(null));
        stats.put("sourceCount", sourceMapper.selectCount(null));
        stats.put("hotCount", hotMapper.selectCount(null));
        stats.put("rankingCount", rankingMapper.selectCount(null));
        stats.put("blogCount", blogMapper.selectCount(null));
        stats.put("imageCount", assetMapper.selectCount(new LambdaQueryWrapper<UploadedAsset>().eq(UploadedAsset::getAssetType, "image")));
        stats.put("searchCount", logMapper.selectCount(null));
        stats.put("behaviorCount", behaviorMapper.selectCount(null));
        stats.put("enabledSources", sourceMapper.selectCount(new LambdaQueryWrapper<SearchSource>().eq(SearchSource::getEnabled, 1)));
        stats.put("enabledHot", hotMapper.selectCount(new LambdaQueryWrapper<HotRecommendation>().eq(HotRecommendation::getEnabled, 1)));
        stats.put("enabledRanking", rankingMapper.selectCount(new LambdaQueryWrapper<RankingConfig>().eq(RankingConfig::getEnabled, 1)));
        return stats;
    }

    public List<AdminUserDto> users() {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getCreateTime));
        return users.stream().map(this::toAdminUserDto).toList();
    }

    public AdminUserDto updateUser(Long id, AdminUserUpdateRequest request) {
        SysUser user = mustGetUser(id);
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname().trim());
        }
        if (StringUtils.hasText(request.getRole())) {
            String role = request.getRole().trim().toUpperCase();
            if (!Set.of("ADMIN", "USER").contains(role)) {
                throw new IllegalArgumentException("角色只能是 ADMIN 或 USER");
            }
            user.setRole(role);
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus() == 1 ? 1 : 0);
        }
        userMapper.updateById(user);
        return toAdminUserDto(mustGetUser(id));
    }

    public PageResult<SearchLog> logs(long page, long size) {
        long current = Math.max(page, 1);
        long pageSize = Math.min(Math.max(size, 10), 100);
        Page<SearchLog> result = logMapper.selectPage(
                Page.of(current, pageSize),
                new LambdaQueryWrapper<SearchLog>().orderByDesc(SearchLog::getCreateTime)
        );
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public boolean deleteLog(Long id) {
        return logMapper.deleteById(id) > 0;
    }

    public Map<String, Object> content() {
        Map<Long, SysUser> users = usersById();
        List<AdminBlogContentDto> blogs = blogMapper.selectList(new LambdaQueryWrapper<BlogPost>()
                        .orderByDesc(BlogPost::getUpdateTime))
                .stream()
                .map(post -> toAdminBlogContentDto(post, users.get(post.getUserId())))
                .toList();
        List<AdminImageContentDto> images = assetMapper.selectList(new LambdaQueryWrapper<UploadedAsset>()
                        .eq(UploadedAsset::getAssetType, "image")
                        .orderByDesc(UploadedAsset::getCreateTime))
                .stream()
                .map(asset -> toAdminImageContentDto(asset, users.get(asset.getUserId())))
                .toList();
        return Map.of("blogs", blogs, "images", images);
    }

    public boolean deleteBlogContent(Long id) {
        mustExistBlog(id);
        return blogMapper.deleteById(id) > 0;
    }

    public AdminBlogContentDto blockBlogContent(Long id, boolean blocked) {
        BlogPost post = mustExistBlog(id);
        post.setBlocked(blocked ? 1 : 0);
        blogMapper.updateById(post);
        return toAdminBlogContentDto(blogMapper.selectById(id), userMapper.selectById(post.getUserId()));
    }

    public boolean deleteImageContent(Long id) {
        mustExistImage(id);
        return assetMapper.deleteById(id) > 0;
    }

    public AdminImageContentDto blockImageContent(Long id, boolean blocked) {
        UploadedAsset asset = mustExistImage(id);
        asset.setBlocked(blocked ? 1 : 0);
        assetMapper.updateById(asset);
        return toAdminImageContentDto(assetMapper.selectById(id), userMapper.selectById(asset.getUserId()));
    }

    public List<SearchSource> sources() {
        return sourceMapper.selectList(new LambdaQueryWrapper<SearchSource>()
                .ne(SearchSource::getType, "web")
                .orderByDesc(SearchSource::getEnabled)
                .orderByDesc(SearchSource::getWeight)
                .orderByDesc(SearchSource::getCreateTime));
    }

    public SearchSource createSource(SearchSource source) {
        normalizeSource(source);
        sourceMapper.insert(source);
        return source;
    }

    public SearchSource updateSource(Long id, SearchSource source) {
        mustExistSource(id);
        source.setId(id);
        normalizeSource(source);
        sourceMapper.updateById(source);
        return sourceMapper.selectById(id);
    }

    public boolean deleteSource(Long id) {
        mustExistSource(id);
        return sourceMapper.deleteById(id) > 0;
    }

    public List<HotRecommendation> hot() {
        return hotMapper.selectList(new LambdaQueryWrapper<HotRecommendation>()
                .orderByDesc(HotRecommendation::getEnabled)
                .orderByDesc(HotRecommendation::getHeatScore)
                .orderByDesc(HotRecommendation::getCreateTime));
    }

    public HotRecommendation createHot(HotRecommendation hot) {
        normalizeHot(hot);
        hotMapper.insert(hot);
        return hot;
    }

    public HotRecommendation updateHot(Long id, HotRecommendation hot) {
        mustExistHot(id);
        hot.setId(id);
        normalizeHot(hot);
        hotMapper.updateById(hot);
        return hotMapper.selectById(id);
    }

    public boolean deleteHot(Long id) {
        mustExistHot(id);
        return hotMapper.deleteById(id) > 0;
    }

    public List<RankingConfig> ranking() {
        return rankingMapper.selectList(new LambdaQueryWrapper<RankingConfig>()
                .orderByDesc(RankingConfig::getEnabled)
                .orderByDesc(RankingConfig::getUpdateTime));
    }

    public RankingConfig createRanking(RankingConfig ranking) {
        normalizeRanking(ranking);
        rankingMapper.insert(ranking);
        return ranking;
    }

    public RankingConfig updateRanking(Long id, RankingConfig ranking) {
        mustExistRanking(id);
        ranking.setId(id);
        normalizeRanking(ranking);
        rankingMapper.updateById(ranking);
        return rankingMapper.selectById(id);
    }

    public boolean deleteRanking(Long id) {
        mustExistRanking(id);
        return rankingMapper.deleteById(id) > 0;
    }

    private AdminUserDto toAdminUserDto(SysUser user) {
        UserProfile profile = profileMapper.selectOne(new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, user.getId()));
        Long behaviorCount = behaviorMapper.selectCount(new LambdaQueryWrapper<UserBehavior>().eq(UserBehavior::getUserId, user.getId()));
        AdminUserDto dto = new AdminUserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setInterestTags(profile == null ? "" : profile.getInterestTags());
        dto.setPreferredTypes(profile == null ? "" : profile.getPreferredTypes());
        dto.setBehaviorCount(behaviorCount);
        dto.setCreateTime(user.getCreateTime());
        dto.setUpdateTime(user.getUpdateTime());
        return dto;
    }

    private AdminBlogContentDto toAdminBlogContentDto(BlogPost post, SysUser user) {
        AdminBlogContentDto dto = new AdminBlogContentDto();
        dto.setId(post.getId());
        dto.setUserId(post.getUserId());
        dto.setUsername(user == null ? "" : user.getUsername());
        dto.setNickname(user == null ? "" : user.getNickname());
        dto.setTitle(post.getTitle());
        dto.setSummary(post.getSummary());
        dto.setContent(post.getContent());
        dto.setStatus(post.getStatus());
        dto.setBlocked(post.getBlocked());
        dto.setTags(post.getTags());
        dto.setCoverUrl(post.getCoverUrl());
        dto.setCreateTime(post.getCreateTime());
        dto.setUpdateTime(post.getUpdateTime());
        return dto;
    }

    private AdminImageContentDto toAdminImageContentDto(UploadedAsset asset, SysUser user) {
        AdminImageContentDto dto = new AdminImageContentDto();
        dto.setId(asset.getId());
        dto.setUserId(asset.getUserId());
        dto.setUsername(user == null ? "" : user.getUsername());
        dto.setNickname(user == null ? "" : user.getNickname());
        dto.setFileName(asset.getFileName());
        dto.setUrl(asset.getUrl());
        dto.setContentType(asset.getContentType());
        dto.setFileSize(asset.getFileSize());
        dto.setTags(asset.getTags());
        dto.setBlocked(asset.getBlocked());
        dto.setCreateTime(asset.getCreateTime());
        return dto;
    }

    private Map<Long, SysUser> usersById() {
        Map<Long, SysUser> users = new LinkedHashMap<>();
        userMapper.selectList(null).forEach(user -> users.put(user.getId(), user));
        return users;
    }

    private SysUser mustGetUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }

    private void mustExistSource(Long id) {
        if (sourceMapper.selectById(id) == null) {
            throw new IllegalArgumentException("搜索源不存在");
        }
    }

    private void mustExistHot(Long id) {
        if (hotMapper.selectById(id) == null) {
            throw new IllegalArgumentException("热点不存在");
        }
    }

    private void mustExistRanking(Long id) {
        if (rankingMapper.selectById(id) == null) {
            throw new IllegalArgumentException("排序配置不存在");
        }
    }

    private BlogPost mustExistBlog(Long id) {
        BlogPost post = blogMapper.selectById(id);
        if (post == null) {
            throw new IllegalArgumentException("博客不存在");
        }
        return post;
    }

    private UploadedAsset mustExistImage(Long id) {
        UploadedAsset asset = assetMapper.selectById(id);
        if (asset == null || !"image".equals(asset.getAssetType())) {
            throw new IllegalArgumentException("图片不存在");
        }
        return asset;
    }

    private void normalizeSource(SearchSource source) {
        if (!StringUtils.hasText(source.getName())) {
            throw new IllegalArgumentException("搜索源名称不能为空");
        }
        if (!StringUtils.hasText(source.getType())) {
            throw new IllegalArgumentException("搜索源类型不能为空");
        }
        source.setName(source.getName().trim());
        source.setType(source.getType().trim());
        source.setEnabled(source.getEnabled() == null || source.getEnabled() == 1 ? 1 : 0);
        source.setWeight(clamp(defaultValue(source.getWeight(), 1.0), 0.0, 10.0));
        source.setAuthorityScore(clamp(defaultValue(source.getAuthorityScore(), 0.7), 0.0, 1.0));
        if (!StringUtils.hasText(source.getConfigJson())) {
            source.setConfigJson("{}");
        }
    }

    private void normalizeHot(HotRecommendation hot) {
        if (!StringUtils.hasText(hot.getTitle())) {
            throw new IllegalArgumentException("热点标题不能为空");
        }
        if (!StringUtils.hasText(hot.getType())) {
            throw new IllegalArgumentException("热点类型不能为空");
        }
        hot.setTitle(hot.getTitle().trim());
        hot.setType(hot.getType().trim());
        hot.setEnabled(hot.getEnabled() == null || hot.getEnabled() == 1 ? 1 : 0);
        hot.setHeatScore(clamp(defaultValue(hot.getHeatScore(), 0.0), 0.0, 100.0));
    }

    private void normalizeRanking(RankingConfig ranking) {
        if (!StringUtils.hasText(ranking.getName())) {
            throw new IllegalArgumentException("排序配置名称不能为空");
        }
        ranking.setName(ranking.getName().trim());
        ranking.setEnabled(ranking.getEnabled() == null || ranking.getEnabled() == 1 ? 1 : 0);
        ranking.setRelevanceWeight(clamp(defaultValue(ranking.getRelevanceWeight(), 0.45), 0.0, 1.0));
        ranking.setFreshnessWeight(clamp(defaultValue(ranking.getFreshnessWeight(), 0.2), 0.0, 1.0));
        ranking.setAuthorityWeight(clamp(defaultValue(ranking.getAuthorityWeight(), 0.25), 0.0, 1.0));
        ranking.setPreferenceWeight(clamp(defaultValue(ranking.getPreferenceWeight(), 0.1), 0.0, 1.0));
        double total = ranking.getRelevanceWeight() + ranking.getFreshnessWeight()
                + ranking.getAuthorityWeight() + ranking.getPreferenceWeight();
        if (Math.abs(total - 1.0) > 0.01) {
            throw new IllegalArgumentException("排序权重总和需要等于 1");
        }
    }

    private double defaultValue(Double value, double defaultValue) {
        return value == null ? defaultValue : value;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
