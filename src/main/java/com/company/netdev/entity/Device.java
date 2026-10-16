package com.company.netdev.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "biz_device")
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** 路由器/交换机/防火墙/无线AP/服务器/负载均衡/UPS/摄像头/其他 */
    @Column(nullable = false, length = 30)
    private String type;

    @Column(length = 50)
    private String brand;

    @Column(length = 80)
    private String model;

    @Column(length = 50)
    private String ip;

    @Column(length = 50)
    private String mac;

    @Column(length = 80)
    private String serialNumber;

    /** IN_USE 在用 / IDLE 闲置 / FAULT 故障 / REPAIR 维修中 / LOANED 已借出 / RETIRED 已报废 */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 50)
    private String manager;

    private LocalDate purchaseDate;

    private LocalDate warrantyExpire;

    @Column(length = 500)
    private String remark;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "location_id")
    private Location location;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
