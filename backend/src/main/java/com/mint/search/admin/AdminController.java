package com.mint.search.admin;

import com.mint.search.admin.dto.AdminUserUpdateRequest;
import com.mint.search.common.ApiResponse;
import com.mint.search.hot.HotRecommendation;
import com.mint.search.ranking.RankingConfig;
import com.mint.search.source.SearchSource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public ApiResponse<?> stats() {
        return ApiResponse.ok(adminService.stats());
    }

    @GetMapping("/users")
    public ApiResponse<?> users() {
        return ApiResponse.ok(adminService.users());
    }

    @PutMapping("/users/{id}")
    public ApiResponse<?> updateUser(@PathVariable Long id, @RequestBody AdminUserUpdateRequest request) {
        return ApiResponse.ok(adminService.updateUser(id, request));
    }

    @GetMapping("/logs")
    public ApiResponse<?> logs(@RequestParam(defaultValue = "1") long page,
                               @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(adminService.logs(page, size));
    }

    @DeleteMapping("/logs/{id}")
    public ApiResponse<?> deleteLog(@PathVariable Long id) {
        return ApiResponse.ok(adminService.deleteLog(id));
    }

    @GetMapping("/content")
    public ApiResponse<?> content() {
        return ApiResponse.ok(adminService.content());
    }

    @DeleteMapping("/content/blogs/{id}")
    public ApiResponse<?> deleteBlogContent(@PathVariable Long id) {
        return ApiResponse.ok(adminService.deleteBlogContent(id));
    }

    @PutMapping("/content/blogs/{id}/blocked")
    public ApiResponse<?> blockBlogContent(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean blocked) {
        return ApiResponse.ok(adminService.blockBlogContent(id, blocked));
    }

    @DeleteMapping("/content/images/{id}")
    public ApiResponse<?> deleteImageContent(@PathVariable Long id) {
        return ApiResponse.ok(adminService.deleteImageContent(id));
    }

    @PutMapping("/content/images/{id}/blocked")
    public ApiResponse<?> blockImageContent(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean blocked) {
        return ApiResponse.ok(adminService.blockImageContent(id, blocked));
    }

    @GetMapping("/sources")
    public ApiResponse<?> sources() {
        return ApiResponse.ok(adminService.sources());
    }

    @PostMapping("/sources")
    public ApiResponse<?> createSource(@RequestBody SearchSource source) {
        return ApiResponse.ok(adminService.createSource(source));
    }

    @PutMapping("/sources/{id}")
    public ApiResponse<?> updateSource(@PathVariable Long id, @RequestBody SearchSource source) {
        return ApiResponse.ok(adminService.updateSource(id, source));
    }

    @DeleteMapping("/sources/{id}")
    public ApiResponse<?> deleteSource(@PathVariable Long id) {
        return ApiResponse.ok(adminService.deleteSource(id));
    }

    @GetMapping("/hot")
    public ApiResponse<?> hot() {
        return ApiResponse.ok(adminService.hot());
    }

    @PostMapping("/hot")
    public ApiResponse<?> createHot(@RequestBody HotRecommendation hot) {
        return ApiResponse.ok(adminService.createHot(hot));
    }

    @PutMapping("/hot/{id}")
    public ApiResponse<?> updateHot(@PathVariable Long id, @RequestBody HotRecommendation hot) {
        return ApiResponse.ok(adminService.updateHot(id, hot));
    }

    @DeleteMapping("/hot/{id}")
    public ApiResponse<?> deleteHot(@PathVariable Long id) {
        return ApiResponse.ok(adminService.deleteHot(id));
    }

    @GetMapping("/ranking")
    public ApiResponse<?> ranking() {
        return ApiResponse.ok(adminService.ranking());
    }

    @PostMapping("/ranking")
    public ApiResponse<?> createRanking(@RequestBody RankingConfig rankingConfig) {
        return ApiResponse.ok(adminService.createRanking(rankingConfig));
    }

    @PutMapping("/ranking/{id}")
    public ApiResponse<?> updateRanking(@PathVariable Long id, @RequestBody RankingConfig rankingConfig) {
        return ApiResponse.ok(adminService.updateRanking(id, rankingConfig));
    }

    @DeleteMapping("/ranking/{id}")
    public ApiResponse<?> deleteRanking(@PathVariable Long id) {
        return ApiResponse.ok(adminService.deleteRanking(id));
    }
}
