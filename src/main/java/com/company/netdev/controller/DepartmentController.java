package com.company.netdev.controller;

import com.company.netdev.common.BizException;
import com.company.netdev.common.Result;
import com.company.netdev.config.AuthInterceptor;
import com.company.netdev.dto.NameRemarkForm;
import com.company.netdev.entity.Department;
import com.company.netdev.repo.DepartmentRepository;
import com.company.netdev.repo.DeviceRepository;
import com.company.netdev.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final DeviceRepository deviceRepository;
    private final LogService logService;

    @GetMapping
    public Result<List<Department>> list() {
        return Result.ok(departmentRepository.findAll());
    }

    @PostMapping
    public Result<Department> create(@RequestBody NameRemarkForm form, HttpServletRequest request) {
        AuthInterceptor.requireAdmin(request);
        if (form.getName() == null || form.getName().isBlank()) throw new BizException("部门名称不能为空");
        Department dept = new Department();
        dept.setName(form.getName().trim());
        dept.setRemark(form.getRemark());
        try {
            departmentRepository.save(dept);
        } catch (Exception e) {
            throw new BizException("部门名称已存在");
        }
        logService.log(AuthInterceptor.currentUserId(request), AuthInterceptor.currentUsername(request),
                "部门管理", "新增", "新增部门 " + dept.getName(), request.getRemoteAddr());
        return Result.ok(dept);
    }

    @PutMapping("/{id}")
    public Result<Department> update(@PathVariable Long id, @RequestBody NameRemarkForm form, HttpServletRequest request) {
        AuthInterceptor.requireAdmin(request);
        Department dept = departmentRepository.findById(id).orElseThrow(() -> new BizException("部门不存在"));
        if (form.getName() != null && !form.getName().isBlank()) dept.setName(form.getName().trim());
        if (form.getRemark() != null) dept.setRemark(form.getRemark());
        try {
            departmentRepository.save(dept);
        } catch (Exception e) {
            throw new BizException("部门名称已存在");
        }
        logService.log(AuthInterceptor.currentUserId(request), AuthInterceptor.currentUsername(request),
                "部门管理", "修改", "修改部门 " + dept.getName(), request.getRemoteAddr());
        return Result.ok(dept);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        AuthInterceptor.requireAdmin(request);
        Department dept = departmentRepository.findById(id).orElseThrow(() -> new BizException("部门不存在"));
        long count = deviceRepository.countByDepartmentId(id);
        if (count > 0) {
            throw new BizException("该部门下还有 " + count + " 台设备，无法删除");
        }
        departmentRepository.delete(dept);
        logService.log(AuthInterceptor.currentUserId(request), AuthInterceptor.currentUsername(request),
                "部门管理", "删除", "删除部门 " + dept.getName(), request.getRemoteAddr());
        return Result.ok();
    }
}
