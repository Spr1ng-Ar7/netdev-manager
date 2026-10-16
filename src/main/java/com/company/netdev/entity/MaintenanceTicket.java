package com.company.netdev.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 维保/维修工单
 * status: PENDING 待受理 / PROCESSING 处理中 / DONE 已完成 / REJECTED 已驳回
 */
@Data
@Entity
@Table(name = "biz_maintenance")
public class MaintenanceTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "device_id")
    private Device device;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "applicant_id")
    private User applicant;

    @Column(nullable = false, length = 1000)
    private String faultDesc;

    @Column(nullable = false, length = 20)
    private String status;

    private LocalDateTime applyTime;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "handler_id")
    private User handler;

    private LocalDateTime acceptTime;

    @Column(length = 1000)
    private String result;

    @Column(precision = 12, scale = 2)
    private BigDecimal cost;

    private LocalDateTime finishTime;

    @PrePersist
    public void prePersist() {
        if (applyTime == null) applyTime = LocalDateTime.now();
    }
}
