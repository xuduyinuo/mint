package com.mint.search.search;

import com.mint.search.common.ApiResponse;
import com.mint.search.search.service.SearchService;
import com.mint.search.security.SecurityUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ApiResponse<?> search(@RequestParam("q") String keyword,
                                 @RequestParam(defaultValue = "all") String type,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "12") int size,
                                 @AuthenticationPrincipal SecurityUser user) {
        Long userId = user == null ? null : user.id();
        return ApiResponse.ok(searchService.search(keyword, type, Math.max(page, 1), Math.min(Math.max(size, 1), 50), userId));
    }
}
