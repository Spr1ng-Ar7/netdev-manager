package com.company.netdev.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InspectionForm {
    private Long deviceId;
    private String result;
    private String content;
    private LocalDateTime inspectTime;
}
