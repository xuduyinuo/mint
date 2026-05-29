package com.mint.search.recommendation;

import com.mint.search.common.ApiResponse;
import com.mint.search.security.SecurityUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ApiResponse<?> recommend(@RequestParam(defaultValue = "all") String type,
                                    @AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(recommendationService.recommend(user == null ? null : user.id(), type));
    }
}
