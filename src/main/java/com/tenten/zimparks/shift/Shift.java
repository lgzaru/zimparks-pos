package com.tenten.zimparks.shift;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity @Table(name = "shifts")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Shift {

    @Id @Column(length = 20)
    private String id;

    @Column(length = 20)
    private String type;

    @Column(nullable = false, length = 10)
    private String status;          // Open | Closed

    @Column(nullable = false, length = 50)
    private String operator;        // username

    @Column(name = "start_time", length = 10)
    private String startTime;

    @Column(name = "end_time", length = 10)
    private String endTime;

    @Column(name = "start_full")
    private LocalDateTime startFull;

    @Column(name = "end_full")
    private LocalDateTime endFull;

    @Column(name = "closed_by", length = 50)
    private String closedBy;
}