package com.tenten.zimparks.bank;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "banks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bank {

    @Id
    @Column(length = 10)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "account_number", length = 30)
    private String accountNumber;
}
