package com.company.netdev.service;

import com.company.netdev.entity.OperationLog;
import com.company.netdev.repo.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogService {

    private final OperationLogRepository logRepository;

    public void log(Long userId, String username, String module, String action, String detail, String ip) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setModule(module);
        log.setAction(action);
        log.setDetail(detail != null && detail.length() > 500 ? detail.substring(0, 500) : detail);
        log.setIp(ip);
        logRepository.save(log);
    }
}
