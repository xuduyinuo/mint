package com.mint.search.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mint.search.common.ApiResponse;
import com.mint.search.hot.HotRecommendation;
import com.mint.search.hot.mapper.HotRecommendationMapper;
import com.mint.search.log.mapper.SearchLogMapper;
import com.mint.search.source.SearchSource;
import com.mint.search.source.mapper.SearchSourceMapper;
import com.mint.search.user.mapper.SysUserMapper;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;
    private final SysUserMapper userMapper;
    private final SearchSourceMapper sourceMapper;
    private final HotRecommendationMapper hotMapper;
    private final SearchLogMapper logMapper;

    public AdminController(AdminService adminService, SysUserMapper userMapper, SearchSourceMapper sourceMapper,
                           HotRecommendationMapper hotMapper, SearchLogMapper logMapper) {
        this.adminService = adminService;
        this.userMapper = userMapper;
        this.sourceMapper = sourceMapper;
        this.hotMapper = hotMapper;
        this.logMapper = logMapper;
    }

    @GetMapping("/stats")
    public ApiResponse<?> stats() {
        return ApiResponse.ok(adminService.stats());
    }

    @GetMapping("/users")
    public ApiResponse<?> users() {
        return ApiResponse.ok(userMapper.selectList(null));
    }

    @GetMapping("/logs")
    public ApiResponse<?> logs() {
        return ApiResponse.ok(logMapper.selectList(null));
    }

    @GetMapping("/sources")
    public ApiResponse<?> sources() {
        return ApiResponse.ok(sourceMapper.selectList(new LambdaQueryWrapper<SearchSource>().ne(SearchSource::getType, "web")));
    }

    @PostMapping("/sources")
    public ApiResponse<?> createSource(@RequestBody SearchSource source) {
        sourceMapper.insert(source);
        return ApiResponse.ok(source);
    }

    @PutMapping("/sources/{id}")
    public ApiResponse<?> updateSource(@PathVariable Long id, @RequestBody SearchSource source) {
        source.setId(id);
        sourceMapper.updateById(source);
        return ApiResponse.ok(source);
    }

    @DeleteMapping("/sources/{id}")
    public ApiResponse<?> deleteSource(@PathVariable Long id) {
        return ApiResponse.ok(sourceMapper.deleteById(id) > 0);
    }

    @GetMapping("/hot")
    public ApiResponse<?> hot() {
        return ApiResponse.ok(hotMapper.selectList(null));
    }

    @PostMapping("/hot")
    public ApiResponse<?> createHot(@RequestBody HotRecommendation hot) {
        hotMapper.insert(hot);
        return ApiResponse.ok(hot);
    }

    @PutMapping("/hot/{id}")
    public ApiResponse<?> updateHot(@PathVariable Long id, @RequestBody HotRecommendation hot) {
        hot.setId(id);
        hotMapper.updateById(hot);
        return ApiResponse.ok(hot);
    }

    @DeleteMapping("/hot/{id}")
    public ApiResponse<?> deleteHot(@PathVariable Long id) {
        return ApiResponse.ok(hotMapper.deleteById(id) > 0);
    }
}
