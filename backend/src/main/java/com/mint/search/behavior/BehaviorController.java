package com.mint.search.behavior;

import com.mint.search.common.ApiResponse;
import com.mint.search.security.SecurityUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/behavior")
public class BehaviorController {
    private final BehaviorService behaviorService;

    public BehaviorController(BehaviorService behaviorService) {
        this.behaviorService = behaviorService;
    }

    @PostMapping
    public ApiResponse<?> record(@AuthenticationPrincipal SecurityUser user, @RequestBody BehaviorRequest request) {
        behaviorService.record(user.id(), request);
        return ApiResponse.ok(true);
    }
}
