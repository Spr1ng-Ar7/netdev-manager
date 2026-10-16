package com.company.netdev.controller;

import com.company.netdev.common.BizException;
import com.company.netdev.common.Result;
import com.company.netdev.config.AuthInterceptor;
import com.company.netdev.dto.InspectionForm;
import com.company.netdev.entity.Device;
import com.company.netdev.entity.InspectionRecord;
import com.company.netdev.entity.UploadImage;
import com.company.netdev.entity.User;
import com.company.netdev.repo.DeviceRepository;
import com.company.netdev.repo.ImageRepository;
import com.company.netdev.repo.InspectionRepository;
import com.company.netdev.repo.UserRepository;
import com.company.netdev.service.FileService;
import com.company.netdev.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionRepository inspectionRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final ImageRepository imageRepository;
    private final FileService fileService;
    private final LogService logService;

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String result) {
        Page<InspectionRecord> data = (result == null || result.isBlank())
                ? inspectionRepository.findAllByOrderByIdDesc(PageRequest.of(page - 1, size))
                : inspectionRepository.findByResultOrderByIdDesc(result, PageRequest.of(page - 1, size));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("list", data.getContent());
        map.put("total", data.getTotalElements());
        return Result.ok(map);
    }

    @PostMapping
    public Result<InspectionRecord> create(@RequestBody InspectionForm form, HttpServletRequest request) {
        if (form.getDeviceId() == null) throw new BizException("请选择设备");
        if (form.getResult() == null || form.getResult().isBlank()) throw new BizException("请选择巡检结果");
        Device device = deviceRepository.findById(form.getDeviceId()).orElseThrow(() -> new BizException("设备不存在"));
        User inspector = userRepository.findById(AuthInterceptor.currentUserId(request))
                .orElseThrow(() -> new BizException(401, "用户不存在"));
        InspectionRecord record = new InspectionRecord();
        record.setDevice(device);
        record.setInspector(inspector);
        record.setResult("NORMAL".equals(form.getResult()) ? "NORMAL" : "ABNORMAL");
        record.setContent(form.getContent());
        record.setInspectTime(form.getInspectTime() == null ? java.time.LocalDateTime.now() : form.getInspectTime());
        inspectionRepository.save(record);
        opLog(request, "登记", "巡检登记：设备 " + device.getName() + "，结果 " + record.getResult());
        return Result.ok(record);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        AuthInterceptor.requireAdmin(request);
        InspectionRecord record = inspectionRepository.findById(id).orElseThrow(() -> new BizException("巡检记录不存在"));
        for (UploadImage img : imageRepository.findByBizTypeAndBizIdOrderByIdAsc("INSPECTION", id)) {
            fileService.delete(img.getStoredName());
        }
        imageRepository.deleteByBizTypeAndBizId("INSPECTION", id);
        inspectionRepository.delete(record);
        opLog(request, "删除", "删除巡检记录 #" + id);
        return Result.ok();
    }

    private void opLog(HttpServletRequest request, String action, String detail) {
        logService.log(AuthInterceptor.currentUserId(request), AuthInterceptor.currentUsername(request),
                "巡检管理", action, detail, request.getRemoteAddr());
    }
}
