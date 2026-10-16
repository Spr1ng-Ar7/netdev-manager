package com.company.netdev.service;

import com.company.netdev.repo.DeviceRepository;
import com.company.netdev.repo.InspectionRepository;
import com.company.netdev.repo.LoanRepository;
import com.company.netdev.repo.MaintenanceRepository;
import com.company.netdev.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DeviceRepository deviceRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final LoanRepository loanRepository;
    private final InspectionRepository inspectionRepository;
    private final UserRepository userRepository;

    public Map<String, Object> stats() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceTotal", deviceRepository.count());
        data.put("deviceInUse", deviceRepository.countByStatus("IN_USE"));
        data.put("deviceFault", deviceRepository.countByStatus("FAULT") + deviceRepository.countByStatus("REPAIR"));
        data.put("maintenanceOpen", maintenanceRepository.countByStatus("PENDING") + maintenanceRepository.countByStatus("PROCESSING"));
        data.put("loanOut", loanRepository.countByStatus("APPROVED"));
        data.put("loanOverdue", loanRepository.findByStatusAndExpectReturnDateBefore("APPROVED", LocalDate.now()).size());
        data.put("inspectionTotal", inspectionRepository.count());
        data.put("userTotal", userRepository.count());
        data.put("deviceByType", toNameValue(deviceRepository.countByType()));
        data.put("deviceByStatus", toNameValue(deviceRepository.countByStatusGroup()));
        data.put("deviceByDepartment", toNameValue(deviceRepository.countByDepartment()));
        data.put("purchaseTrend", purchaseTrend());
        return data;
    }

    private List<Map<String, Object>> toNameValue(List<Object[]> rows) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", row[0]);
            m.put("value", row[1]);
            list.add(m);
        }
        return list;
    }

    /** 近 12 个月设备采购趋势（缺月补 0） */
    private List<Map<String, Object>> purchaseTrend() {
        Map<String, Long> db = new LinkedHashMap<>();
        for (Object[] row : deviceRepository.countByPurchaseMonth()) {
            db.put((String) row[0], ((Number) row[1]).longValue());
        }
        List<Map<String, Object>> list = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        YearMonth ym = YearMonth.now().minusMonths(11);
        for (int i = 0; i < 12; i++) {
            String key = ym.format(fmt);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", key);
            m.put("count", db.getOrDefault(key, 0L));
            list.add(m);
            ym = ym.plusMonths(1);
        }
        return list;
    }
}
