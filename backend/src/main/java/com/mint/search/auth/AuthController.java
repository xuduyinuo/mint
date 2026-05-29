package com.mint.search.auth;

import com.mint.search.auth.dto.LoginRequest;
import com.mint.search.auth.dto.RegisterRequest;
import com.mint.search.common.ApiResponse;
import com.mint.search.security.SecurityUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<?> me(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(authService.findById(user.id()));
    }
}
