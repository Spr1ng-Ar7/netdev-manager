package com.company.netdev.controller;

import com.company.netdev.common.BizException;
import com.company.netdev.common.Result;
import com.company.netdev.config.AuthInterceptor;
import com.company.netdev.dto.MaintenanceForm;
import com.company.netdev.entity.Device;
import com.company.netdev.entity.MaintenanceTicket;
import com.company.netdev.entity.User;
import com.company.netdev.repo.DeviceRepository;
import com.company.netdev.repo.MaintenanceRepository;
import com.company.netdev.repo.UserRepository;
import com.company.netdev.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenances")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceRepository maintenanceRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final LogService logService;

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String status) {
        Page<MaintenanceTicket> result = (status == null || status.isBlank())
                ? maintenanceRepository.findAllByOrderByIdDesc(PageRequest.of(page - 1, size))
                : maintenanceRepository.findByStatusOrderByIdDesc(status, PageRequest.of(page - 1, size));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", result.getContent());
        data.put("total", result.getTotalElements());
        return Result.ok(data);
    }

    /** 提交维保申请（任何登录用户） */
    @PostMapping
    public Result<MaintenanceTicket> apply(@RequestBody MaintenanceForm form, HttpServletRequest request) {
        if (form.getDeviceId() == null) throw new BizException("请选择设备");
        if (form.getFaultDesc() == null || form.getFaultDesc().isBlank()) throw new BizException("请填写故障描述");
        Device device = deviceRepository.findById(form.getDeviceId()).orElseThrow(() -> new BizException("设备不存在"));
        if ("RETIRED".equals(device.getStatus())) throw new BizException("已报废设备不能申请维保");
        User applicant = currentUser(request);
        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setDevice(device);
        ticket.setApplicant(applicant);
        ticket.setFaultDesc(form.getFaultDesc().trim());
        ticket.setStatus("PENDING");
        maintenanceRepository.save(ticket);
        device.setStatus("REPAIR");
        deviceRepository.save(device);
        opLog(request, "申请", "提交维保申请：设备 " + device.getName());
        return Result.ok(ticket);
    }

    /** 受理（管理员） */
    @PutMapping("/{id}/accept")
    public Result<MaintenanceTicket> accept(@PathVariable Long id, HttpServletRequest request) {
        AuthInterceptor.requireAdmin(request);
        MaintenanceTicket ticket = get(id);
        if (!"PENDING".equals(ticket.getStatus())) throw new BizException("仅待受理工单可受理");
        ticket.setStatus("PROCESSING");
        ticket.setHandler(currentUser(request));
        ticket.setAcceptTime(LocalDateTime.now());
        maintenanceRepository.save(ticket);
        opLog(request, "受理", "受理维保工单 #" + id);
        return Result.ok(ticket);
    }

    /** 完成（管理员） */
    @PutMapping("/{id}/finish")
    public Result<MaintenanceTicket> finish(@PathVariable Long id, @RequestBody MaintenanceForm form, HttpServletRequest request) {
        AuthInterceptor.requireAdmin(request);
        MaintenanceTicket ticket = get(id);
        if (!"PROCESSING".equals(ticket.getStatus()) && !"PENDING".equals(ticket.getStatus())) {
            throw new BizException("该工单状态不能完成");
        }
        if (form.getResult() == null || form.getResult().isBlank()) throw new BizException("请填写维修结果");
        ticket.setStatus("DONE");
        ticket.setResult(form.getResult().trim());
        ticket.setCost(form.getCost());
        ticket.setFinishTime(LocalDateTime.now());
        if (ticket.getHandler() == null) {
            ticket.setHandler(currentUser(request));
            ticket.setAcceptTime(LocalDateTime.now());
        }
        maintenanceRepository.save(ticket);
        Device device = ticket.getDevice();
        if (device != null && "REPAIR".equals(device.getStatus())) {
            device.setStatus("IN_USE");
            deviceRepository.save(device);
        }
        opLog(request, "完成", "完成维保工单 #" + id);
        return Result.ok(ticket);
    }

    /** 驳回（管理员） */
    @PutMapping("/{id}/reject")
    public Result<MaintenanceTicket> reject(@PathVariable Long id, @RequestBody MaintenanceForm form, HttpServletRequest request) {
        AuthInterceptor.requireAdmin(request);
        MaintenanceTicket ticket = get(id);
        if (!"PENDING".equals(ticket.getStatus())) throw new BizException("仅待受理工单可驳回");
        ticket.setStatus("REJECTED");
        ticket.setResult(form.getResult() == null ? "驳回" : form.getResult().trim());
        ticket.setHandler(currentUser(request));
        ticket.setAcceptTime(LocalDateTime.now());
        maintenanceRepository.save(ticket);
        Device device = ticket.getDevice();
        if (device != null && "REPAIR".equals(device.getStatus())) {
            device.setStatus("IN_USE");
            deviceRepository.save(device);
        }
        opLog(request, "驳回", "驳回维保工单 #" + id);
        return Result.ok(ticket);
    }

    private MaintenanceTicket get(Long id) {
        return maintenanceRepository.findById(id).orElseThrow(() -> new BizException("工单不存在"));
    }

    private User currentUser(HttpServletRequest request) {
        return userRepository.findById(AuthInterceptor.currentUserId(request))
                .orElseThrow(() -> new BizException(401, "用户不存在"));
    }

    private void opLog(HttpServletRequest request, String action, String detail) {
        logService.log(AuthInterceptor.currentUserId(request), AuthInterceptor.currentUsername(request),
                "维保维修", action, detail, request.getRemoteAddr());
    }
}
