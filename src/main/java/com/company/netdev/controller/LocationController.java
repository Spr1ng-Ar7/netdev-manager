package com.company.netdev.controller;

import com.company.netdev.common.BizException;
import com.company.netdev.common.Result;
import com.company.netdev.config.AuthInterceptor;
import com.company.netdev.dto.NameRemarkForm;
import com.company.netdev.entity.Location;
import com.company.netdev.repo.DeviceRepository;
import com.company.netdev.repo.LocationRepository;
import com.company.netdev.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationRepository locationRepository;
    private final DeviceRepository deviceRepository;
    private final LogService logService;

    @GetMapping
    public Result<List<Location>> list() {
        return Result.ok(locationRepository.findAll());
    }

    @PostMapping
    public Result<Location> create(@RequestBody NameRemarkForm form, HttpServletRequest request) {
        AuthInterceptor.requireAdmin(request);
        if (form.getName() == null || form.getName().isBlank()) throw new BizException("位置名称不能为空");
        Location loc = new Location();
        loc.setName(form.getName().trim());
        loc.setRemark(form.getRemark());
        try {
            locationRepository.save(loc);
        } catch (Exception e) {
            throw new BizException("位置名称已存在");
        }
        logService.log(AuthInterceptor.currentUserId(request), AuthInterceptor.currentUsername(request),
                "位置管理", "新增", "新增位置 " + loc.getName(), request.getRemoteAddr());
        return Result.ok(loc);
    }

    @PutMapping("/{id}")
    public Result<Location> update(@PathVariable Long id, @RequestBody NameRemarkForm form, HttpServletRequest request) {
        AuthInterceptor.requireAdmin(request);
        Location loc = locationRepository.findById(id).orElseThrow(() -> new BizException("位置不存在"));
        if (form.getName() != null && !form.getName().isBlank()) loc.setName(form.getName().trim());
        if (form.getRemark() != null) loc.setRemark(form.getRemark());
        try {
            locationRepository.save(loc);
        } catch (Exception e) {
            throw new BizException("位置名称已存在");
        }
        logService.log(AuthInterceptor.currentUserId(request), AuthInterceptor.currentUsername(request),
                "位置管理", "修改", "修改位置 " + loc.getName(), request.getRemoteAddr());
        return Result.ok(loc);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        AuthInterceptor.requireAdmin(request);
        Location loc = locationRepository.findById(id).orElseThrow(() -> new BizException("位置不存在"));
        long count = deviceRepository.countByLocationId(id);
        if (count > 0) {
            throw new BizException("该位置下还有 " + count + " 台设备，无法删除");
        }
        locationRepository.delete(loc);
        logService.log(AuthInterceptor.currentUserId(request), AuthInterceptor.currentUsername(request),
                "位置管理", "删除", "删除位置 " + loc.getName(), request.getRemoteAddr());
        return Result.ok();
    }
}
