package com.tenten.zimparks.creditnote;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "credit_notes")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditNote {

    @Id @Column(length = 20)
    private String id;

    @Column(name = "tx_ref", length = 30)
    private String txRef;

    @Column(length = 100)
    private String client;

    @Column(length = 255)
    @Enumerated(EnumType.STRING)
    private CreditNoteReason reason;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 20)
    private String status;          // PENDING | APPROVED | REJECTED

    @Column(name = "note_date", length = 20)
    private String noteDate;

    @Column(name = "notice_date")
    private LocalDate noticeDate;

    @Column(name = "deduction_percentage", precision = 5, scale = 2)
    private BigDecimal deductionPercentage;

    @Column(name = "full_value", precision = 10, scale = 2)
    private BigDecimal fullValue;

    @Column(name = "raised_by", length = 50)
    private String raisedBy;

    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    @Column(name = "raised_by_role", length = 20)
    private String raisedByRole;

    @Column(name = "shift_id", length = 20)
    private String shiftId;

    // Fiscalization fields — populated on successful ZIMRA CreditNote submission
    @Column(name = "fiscal_status", length = 40)
    private String fiscalStatus;

    @Column(name = "fiscal_receipt_id")
    private Long fiscalReceiptId;

    @Column(name = "fiscal_operation_id", length = 80)
    private String fiscalOperationId;

    @Column(name = "fiscal_error", length = 500)
    private String fiscalError;
}
