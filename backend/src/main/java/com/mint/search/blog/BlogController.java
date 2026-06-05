package com.mint.search.blog;

import com.mint.search.blog.dto.BlogPostRequest;
import com.mint.search.common.ApiResponse;
import com.mint.search.security.SecurityUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/blogs")
public class BlogController {
    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping
    public ApiResponse<?> published() {
        return ApiResponse.ok(blogService.published());
    }

    @GetMapping("/mine")
    public ApiResponse<?> mine(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(blogService.mine(user.id()));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody BlogPostRequest request,
                                 @AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(blogService.create(user.id(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id,
                                 @RequestBody BlogPostRequest request,
                                 @AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(blogService.update(user.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id,
                                 @AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(blogService.delete(user.id(), id));
    }
}
