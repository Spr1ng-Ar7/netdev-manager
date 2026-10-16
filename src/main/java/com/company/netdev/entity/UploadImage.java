package com.company.netdev.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一图片附件表：通过 bizType + bizId 关联各业务模块
 * bizType: DEVICE 设备 / MAINT_REQUEST 维保申请 / MAINT_RESULT 维修结果 /
 *          INSPECTION 巡检记录 / LOAN 借还记录
 */
@Data
@Entity
@Table(name = "biz_image", indexes = {
        @Index(name = "idx_image_biz", columnList = "bizType,bizId")
})
public class UploadImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String bizType;

    @Column(nullable = false)
    private Long bizId;

    @Column(nullable = false, length = 100)
    private String storedName;

    @Column(length = 200)
    private String originalName;

    private Long size;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @Transient
    public String getUrl() {
        return "/uploads/" + storedName;
    }
}
