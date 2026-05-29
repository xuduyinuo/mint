package com.mint.search.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mint.search.auth.dto.AuthResponse;
import com.mint.search.auth.dto.LoginRequest;
import com.mint.search.auth.dto.RegisterRequest;
import com.mint.search.security.JwtService;
import com.mint.search.user.SysUser;
import com.mint.search.user.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(SysUserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("账号和密码不能为空");
        }
        SysUser exists = findByUsername(request.getUsername());
        if (exists != null) {
            throw new IllegalArgumentException("账号已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);
        return toResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        SysUser user = findByUsername(request.getUsername());
        if (user == null || user.getStatus() == 0 || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        return toResponse(user);
    }

    public SysUser findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username).last("limit 1"));
    }

    public SysUser findById(Long id) {
        return userMapper.selectById(id);
    }

    private AuthResponse toResponse(SysUser user) {
        String token = jwtService.createToken(user.getId(), user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getNickname(), user.getRole());
    }
}
