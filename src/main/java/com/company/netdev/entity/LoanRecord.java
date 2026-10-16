package com.company.netdev.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备借出/归还
 * status: PENDING 待审批 / APPROVED 已借出 / REJECTED 已驳回 / RETURNED 已归还
 */
@Data
@Entity
@Table(name = "biz_loan")
public class LoanRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "device_id")
    private Device device;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "applicant_id")
    private User applicant;

    @Column(nullable = false, length = 50)
    private String borrower;

    @Column(length = 300)
    private String purpose;

    @Column(nullable = false)
    private LocalDate expectReturnDate;

    @Column(nullable = false, length = 20)
    private String status;

    private LocalDateTime applyTime;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "approver_id")
    private User approver;

    private LocalDateTime approveTime;

    @Column(length = 300)
    private String approveRemark;

    private LocalDate returnDate;

    @Column(length = 300)
    private String returnRemark;

    @PrePersist
    public void prePersist() {
        if (applyTime == null) applyTime = LocalDateTime.now();
    }
}
