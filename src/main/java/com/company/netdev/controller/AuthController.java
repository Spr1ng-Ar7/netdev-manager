package com.company.netdev.controller;

import com.company.netdev.common.BizException;
import com.company.netdev.common.PasswordUtil;
import com.company.netdev.common.Result;
import com.company.netdev.config.AuthInterceptor;
import com.company.netdev.entity.User;
import com.company.netdev.repo.UserRepository;
import com.company.netdev.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final LogService logService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BizException("请输入用户名和密码");
        }
        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new BizException("用户名或密码错误"));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BizException("账号已被禁用，请联系管理员");
        }
        if (!PasswordUtil.verify(password, user.getSalt(), user.getPasswordHash())) {
            throw new BizException("用户名或密码错误");
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(AuthInterceptor.SESSION_USER_ID, user.getId());
        session.setAttribute(AuthInterceptor.SESSION_USERNAME, user.getUsername());
        session.setAttribute(AuthInterceptor.SESSION_NAME, user.getName());
        session.setAttribute(AuthInterceptor.SESSION_ROLE, user.getRole());
        logService.log(user.getId(), user.getUsername(), "认证", "登录", "用户登录系统", request.getRemoteAddr());
        return Result.ok(toMe(user));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me(HttpServletRequest request) {
        Long userId = AuthInterceptor.currentUserId(request);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(401, "用户不存在"));
        return Result.ok(toMe(user));
    }

    private Map<String, Object> toMe(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("name", user.getName());
        m.put("role", user.getRole());
        return m;
    }
}
