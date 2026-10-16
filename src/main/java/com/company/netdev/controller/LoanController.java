package com.company.netdev.controller;

import com.company.netdev.common.BizException;
import com.company.netdev.common.Result;
import com.company.netdev.config.AuthInterceptor;
import com.company.netdev.dto.LoanForm;
import com.company.netdev.entity.Device;
import com.company.netdev.entity.LoanRecord;
import com.company.netdev.entity.User;
import com.company.netdev.repo.DeviceRepository;
import com.company.netdev.repo.LoanRepository;
import com.company.netdev.repo.UserRepository;
import com.company.netdev.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanRepository loanRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final LogService logService;

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String status) {
        Page<LoanRecord> result = (status == null || status.isBlank())
                ? loanRepository.findAllByOrderByIdDesc(PageRequest.of(page - 1, size))
                : loanRepository.findByStatusOrderByIdDesc(status, PageRequest.of(page - 1, size));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", result.getContent());
        data.put("total", result.getTotalElements());
        return Result.ok(data);
    }

    /** 提交借出申请（任何登录用户） */
    @PostMapping
    public Result<LoanRecord> apply(@RequestBody LoanForm form, HttpServletRequest request) {
        if (form.getDeviceId() == null) throw new BizException("请选择设备");
        if (form.getBorrower() == null || form.getBorrower().isBlank()) throw new BizException("请填写借用人");
        if (form.getExpectReturnDate() == null) throw new BizException("请选择预计归还日期");
        if (form.getExpectReturnDate().isBefore(LocalDate.now())) throw new BizException("预计归还日期不能早于今天");
        Device device = deviceRepository.findById(form.getDeviceId()).orElseThrow(() -> new BizException("设备不存在"));
        if (!"IN_USE".equals(device.getStatus()) && !"IDLE".equals(device.getStatus())) {
            throw new BizException("设备当前状态（" + device.getStatus() + "）不可借出");
        }
        if (loanRepository.existsByDeviceIdAndStatus(device.getId(), "PENDING")
                || loanRepository.existsByDeviceIdAndStatus(device.getId(), "APPROVED")) {
            throw new BizException("该设备已有未完结的借还记录");
        }
        User applicant = currentUser(request);
        LoanRecord loan = new LoanRecord();
        loan.setDevice(device);
        loan.setApplicant(applicant);
        loan.setBorrower(form.getBorrower().trim());
        loan.setPurpose(form.getPurpose());
        loan.setExpectReturnDate(form.getExpectReturnDate());
        loan.setStatus("PENDING");
        loanRepository.save(loan);
        opLog(request, "申请", "提交借出申请：设备 " + device.getName());
        return Result.ok(loan);
    }

    /** 审批通过（管理员），设备置为已借出 */
    @PutMapping("/{id}/approve")
    public Result<LoanRecord> approve(@PathVariable Long id, @RequestBody(required = false) LoanForm form, HttpServletRequest request) {
        AuthInterceptor.requireAdmin(request);
        LoanRecord loan = get(id);
        if (!"PENDING".equals(loan.getStatus())) throw new BizException("仅待审批申请可审批");
        loan.setStatus("APPROVED");
        loan.setApprover(currentUser(request));
        loan.setApproveTime(LocalDateTime.now());
        if (form != null && form.getRemark() != null) loan.setApproveRemark(form.getRemark());
        loanRepository.save(loan);
        Device device = loan.getDevice();
        device.setStatus("LOANED");
        deviceRepository.save(device);
        opLog(request, "审批通过", "借出审批通过：设备 " + device.getName());
        return Result.ok(loan);
    }

    /** 审批驳回（管理员） */
    @PutMapping("/{id}/reject")
    public Result<LoanRecord> reject(@PathVariable Long id, @RequestBody(required = false) LoanForm form, HttpServletRequest request) {
        AuthInterceptor.requireAdmin(request);
        LoanRecord loan = get(id);
        if (!"PENDING".equals(loan.getStatus())) throw new BizException("仅待审批申请可审批");
        loan.setStatus("REJECTED");
        loan.setApprover(currentUser(request));
        loan.setApproveTime(LocalDateTime.now());
        if (form != null && form.getRemark() != null) loan.setApproveRemark(form.getRemark());
        loanRepository.save(loan);
        opLog(request, "驳回", "借出申请驳回 #" + id);
        return Result.ok(loan);
    }

    /** 归还登记，设备恢复在用 */
    @PutMapping("/{id}/return")
    public Result<LoanRecord> returnBack(@PathVariable Long id, @RequestBody(required = false) LoanForm form, HttpServletRequest request) {
        LoanRecord loan = get(id);
        if (!"APPROVED".equals(loan.getStatus())) throw new BizException("仅已借出记录可登记归还");
        loan.setStatus("RETURNED");
        loan.setReturnDate(LocalDate.now());
        if (form != null && form.getRemark() != null) loan.setReturnRemark(form.getRemark());
        loanRepository.save(loan);
        Device device = loan.getDevice();
        if ("LOANED".equals(device.getStatus())) {
            device.setStatus("IN_USE");
            deviceRepository.save(device);
        }
        opLog(request, "归还", "设备归还登记：" + device.getName());
        return Result.ok(loan);
    }

    private LoanRecord get(Long id) {
        return loanRepository.findById(id).orElseThrow(() -> new BizException("借还记录不存在"));
    }

    private User currentUser(HttpServletRequest request) {
        return userRepository.findById(AuthInterceptor.currentUserId(request))
                .orElseThrow(() -> new BizException(401, "用户不存在"));
    }

    private void opLog(HttpServletRequest request, String action, String detail) {
        logService.log(AuthInterceptor.currentUserId(request), AuthInterceptor.currentUsername(request),
                "借还管理", action, detail, request.getRemoteAddr());
    }
}
