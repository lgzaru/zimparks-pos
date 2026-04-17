package com.tenten.zimparks.cashup;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Records the amounts declared by an operator when closing their own shift.
 * Status flow: PENDING_REVIEW → APPROVED | WAIVED
 *
 * APPROVED — no shortfalls, declared matches actuals.
 * WAIVED   — supervisor approved despite shortfalls (notes mandatory); declared amounts
 *            may be updated by the supervisor to reflect rectified/recovered amounts.
 */
@Entity
@Table(name = "cashup_submissions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CashupSubmission {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shift_id", nullable = false, length = 20)
    private String shiftId;

    @Column(name = "submitted_by", nullable = false, length = 50)
    private String submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    // PENDING_REVIEW | APPROVED | WAIVED
    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "reviewed_by", length = 50)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewer_notes", columnDefinition = "TEXT")
    private String reviewerNotes;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "cashup_id")
    private List<CashupLine> lines;
}
