package com.company.netdev.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 巡检记录
 * result: NORMAL 正常 / ABNORMAL 异常
 */
@Data
@Entity
@Table(name = "biz_inspection")
public class InspectionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "device_id")
    private Device device;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "inspector_id")
    private User inspector;

    @Column(nullable = false, length = 20)
    private String result;

    @Column(length = 1000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime inspectTime;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (inspectTime == null) inspectTime = LocalDateTime.now();
    }
}
