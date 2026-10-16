package com.company.netdev.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DeviceForm {
    private String name;
    private String type;
    private String brand;
    private String model;
    private String ip;
    private String mac;
    private String serialNumber;
    private String status;
    private String manager;
    private LocalDate purchaseDate;
    private LocalDate warrantyExpire;
    private String remark;
    private Long departmentId;
    private Long locationId;
}
