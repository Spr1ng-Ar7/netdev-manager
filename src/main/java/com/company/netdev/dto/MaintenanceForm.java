package com.company.netdev.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaintenanceForm {
    private Long deviceId;
    private String faultDesc;
    /** 完成时填写 */
    private String result;
    private BigDecimal cost;
}
