package com.company.netdev.controller;

import com.company.netdev.common.Result;
import com.company.netdev.config.AuthInterceptor;
import com.company.netdev.entity.OperationLog;
import com.company.netdev.repo.OperationLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final OperationLogRepository logRepository;

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String module,
                                            @RequestParam(required = false) String keyword,
                                            HttpServletRequest request) {
        AuthInterceptor.requireAdmin(request);
        PageRequest pageable = PageRequest.of(page - 1, size);
        Page<OperationLog> data;
        if (module != null && !module.isBlank()) {
            data = logRepository.findByModuleOrderByCreatedAtDesc(module, pageable);
        } else if (keyword != null && !keyword.isBlank()) {
            data = logRepository.findByDetailContainingOrderByCreatedAtDesc(keyword.trim(), pageable);
        } else {
            data = logRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("list", data.getContent());
        map.put("total", data.getTotalElements());
        return Result.ok(map);
    }
}
