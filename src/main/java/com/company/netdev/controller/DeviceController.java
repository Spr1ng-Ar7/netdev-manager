package com.company.netdev.controller;

import com.company.netdev.common.BizException;
import com.company.netdev.common.Result;
import com.company.netdev.config.AuthInterceptor;
import com.company.netdev.dto.DeviceForm;
import com.company.netdev.entity.Device;
import com.company.netdev.entity.Department;
import com.company.netdev.entity.Location;
import com.company.netdev.entity.UploadImage;
import com.company.netdev.repo.*;
import com.company.netdev.service.FileService;
import com.company.netdev.service.LogService;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceRepository deviceRepository;
    private final DepartmentRepository departmentRepository;
    private final LocationRepository locationRepository;
    private final ImageRepository imageRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final LoanRepository loanRepository;
    private final InspectionRepository inspectionRepository;
    private final FileService fileService;
    private final LogService logService;

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String type,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) Long departmentId,
                                            @RequestParam(required = false) Long locationId) {
        Specification<Device> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword.trim() + "%";
                ps.add(cb.or(
                        cb.like(root.get("name"), kw),
                        cb.like(root.get("ip"), kw),
                        cb.like(root.get("mac"), kw),
                        cb.like(root.get("serialNumber"), kw),
                        cb.like(root.get("manager"), kw),
                        cb.like(root.get("brand"), kw),
                        cb.like(root.get("model"), kw)));
            }
            if (type != null && !type.isBlank()) ps.add(cb.equal(root.get("type"), type));
            if (status != null && !status.isBlank()) ps.add(cb.equal(root.get("status"), status));
            if (departmentId != null) ps.add(cb.equal(root.get("department").get("id"), departmentId));
            if (locationId != null) ps.add(cb.equal(root.get("location").get("id"), locationId));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Device> result = deviceRepository.findAll(spec,
                PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "id")));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", result.getContent());
        data.put("total", result.getTotalElements());
        return Result.ok(data);
    }

    /** 下拉选项用的精简列表 */
    @GetMapping("/options")
    public Result<List<Map<String, Object>>> options() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Device d : deviceRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("name", d.getName());
            m.put("type", d.getType());
            m.put("status", d.getStatus());
            list.add(m);
        }
        return Result.ok(list);
    }

    /** 详情：设备信息 + 图片 + 维修历史 */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Device device = deviceRepository.findById(id).orElseThrow(() -> new BizException("设备不存在"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("device", device);
        data.put("images", imageRepository.findByBizTypeAndBizIdOrderByIdAsc("DEVICE", id));
        data.put("maintenances", maintenanceRepository.findByDeviceIdOrderByIdDesc(id));
        return Result.ok(data);
    }

    @PostMapping
    public Result<Device> create(@RequestBody DeviceForm form, HttpServletRequest request) {
        AuthInterceptor.requireAdmin(request);
        Device device = new Device();
        applyForm(device, form);
        deviceRepository.save(device);
        opLog(request, "新增", "新增设备 " + device.getName());
        return Result.ok(device);
    }

    @PutMapping("/{id}")
    public Result<Device> update(@PathVariable Long id, @RequestBody DeviceForm form, HttpServletRequest request) {
        AuthInterceptor.requireAdmin(request);
        Device device = deviceRepository.findById(id).orElseThrow(() -> new BizException("设备不存在"));
        applyForm(device, form);
        deviceRepository.save(device);
        opLog(request, "修改", "修改设备 " + device.getName());
        return Result.ok(device);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        AuthInterceptor.requireAdmin(request);
        Device device = deviceRepository.findById(id).orElseThrow(() -> new BizException("设备不存在"));
        // 级联清理关联业务数据与图片
        maintenanceRepository.deleteAll(maintenanceRepository.findByDeviceIdOrderByIdDesc(id));
        loanRepository.deleteAll(loanRepository.findByDeviceId(id));
        inspectionRepository.deleteAll(inspectionRepository.findByDeviceId(id));
        for (UploadImage img : imageRepository.findByBizTypeAndBizIdOrderByIdAsc("DEVICE", id)) {
            fileService.delete(img.getStoredName());
        }
        imageRepository.deleteByBizTypeAndBizId("DEVICE", id);
        deviceRepository.delete(device);
        opLog(request, "删除", "删除设备 " + device.getName());
        return Result.ok();
    }

    private void applyForm(Device device, DeviceForm form) {
        if (form.getName() == null || form.getName().isBlank()) throw new BizException("设备名称不能为空");
        if (form.getType() == null || form.getType().isBlank()) throw new BizException("设备类型不能为空");
        device.setName(form.getName().trim());
        device.setType(form.getType().trim());
        device.setBrand(form.getBrand());
        device.setModel(form.getModel());
        device.setIp(form.getIp());
        device.setMac(form.getMac());
        device.setSerialNumber(form.getSerialNumber());
        device.setStatus(form.getStatus() == null || form.getStatus().isBlank() ? "IDLE" : form.getStatus());
        device.setManager(form.getManager());
        device.setPurchaseDate(form.getPurchaseDate());
        device.setWarrantyExpire(form.getWarrantyExpire());
        device.setRemark(form.getRemark());
        Department dept = null;
        if (form.getDepartmentId() != null) {
            dept = departmentRepository.findById(form.getDepartmentId())
                    .orElseThrow(() -> new BizException("所选部门不存在"));
        }
        device.setDepartment(dept);
        Location loc = null;
        if (form.getLocationId() != null) {
            loc = locationRepository.findById(form.getLocationId())
                    .orElseThrow(() -> new BizException("所选位置不存在"));
        }
        device.setLocation(loc);
    }

    private void opLog(HttpServletRequest request, String action, String detail) {
        logService.log(AuthInterceptor.currentUserId(request), AuthInterceptor.currentUsername(request),
                "设备管理", action, detail, request.getRemoteAddr());
    }
}
