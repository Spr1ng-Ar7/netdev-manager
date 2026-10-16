package com.company.netdev.controller;

import com.company.netdev.common.BizException;
import com.company.netdev.common.PasswordUtil;
import com.company.netdev.common.Result;
import com.company.netdev.config.AuthInterceptor;
import com.company.netdev.dto.UserForm;
import com.company.netdev.entity.User;
import com.company.netdev.repo.UserRepository;
import com.company.netdev.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final LogService logService;

    @GetMapping
    public Result<List<User>> list(@RequestParam(required = false) String keyword) {
        List<User> users;
        if (keyword != null && !keyword.isBlank()) {
            users = userRepository.findByNameContainingOrUsernameContaining(keyword.trim(), keyword.trim());
        } else {
            users = userRepository.findAll();
        }
        return Result.ok(users);
    }

    @PostMapping
    public Result<User> create(@RequestBody UserForm form, HttpServletRequest request) {
        AuthInterceptor.requireAdmin(request);
        if (form.getUsername() == null || form.getUsername().isBlank()) throw new BizException("用户名不能为空");
        if (form.getPassword() == null || form.getPassword().length() < 6) throw new BizException("密码至少 6 位");
        if (form.getName() == null || form.getName().isBlank()) throw new BizException("姓名不能为空");
        if (userRepository.existsByUsername(form.getUsername().trim())) throw new BizException("用户名已存在");
        User user = new User();
        user.setUsername(form.getUsername().trim());
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        user.setSalt(salt);
        user.setPasswordHash(PasswordUtil.hash(form.getPassword(), salt));
        user.setName(form.getName().trim());
        user.setRole("ADMIN".equals(form.getRole()) ? "ADMIN" : "USER");
        user.setPhone(form.getPhone());
        user.setEmail(form.getEmail());
        user.setEnabled(form.getEnabled() == null ? Boolean.TRUE : form.getEnabled());
        userRepository.save(user);
        opLog(request, "新增", "新增用户 " + user.getUsername());
        return Result.ok(user);
    }

    @PutMapping("/{id}")
    public Result<User> update(@PathVariable Long id, @RequestBody UserForm form, HttpServletRequest request) {
        AuthInterceptor.requireAdmin(request);
        User user = userRepository.findById(id).orElseThrow(() -> new BizException("用户不存在"));
        if (form.getName() != null && !form.getName().isBlank()) user.setName(form.getName().trim());
        if (form.getRole() != null) user.setRole("ADMIN".equals(form.getRole()) ? "ADMIN" : "USER");
        if (form.getPhone() != null) user.setPhone(form.getPhone());
        if (form.getEmail() != null) user.setEmail(form.getEmail());
        if (form.getEnabled() != null) {
            if (id.equals(AuthInterceptor.currentUserId(request)) && !form.getEnabled()) {
                throw new BizException("不能禁用自己的账号");
            }
            user.setEnabled(form.getEnabled());
        }
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            if (form.getPassword().length() < 6) throw new BizException("密码至少 6 位");
            String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            user.setSalt(salt);
            user.setPasswordHash(PasswordUtil.hash(form.getPassword(), salt));
        }
        userRepository.save(user);
        opLog(request, "修改", "修改用户 " + user.getUsername());
        return Result.ok(user);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        AuthInterceptor.requireAdmin(request);
        if (id.equals(AuthInterceptor.currentUserId(request))) {
            throw new BizException("不能删除自己的账号");
        }
        User user = userRepository.findById(id).orElseThrow(() -> new BizException("用户不存在"));
        userRepository.delete(user);
        opLog(request, "删除", "删除用户 " + user.getUsername());
        return Result.ok();
    }

    private void opLog(HttpServletRequest request, String action, String detail) {
        logService.log(AuthInterceptor.currentUserId(request), AuthInterceptor.currentUsername(request),
                "用户管理", action, detail, request.getRemoteAddr());
    }
}
