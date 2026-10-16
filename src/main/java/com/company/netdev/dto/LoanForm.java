package com.company.netdev.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LoanForm {
    private Long deviceId;
    private String borrower;
    private String purpose;
    private LocalDate expectReturnDate;
    /** 审批/归还备注 */
    private String remark;
}
