package com.tenten.zimparks.customer;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "customers")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Customer {

    @Id @Column(length = 20)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 20)
    private String type;          // Individual | Corporate

    @Column(length = 50)
    private String nationality;

    @Column(length = 20)
    private String tin;           // Tax Identification Number — required for ZIMRA buyerData (RCPT043)

    @Column(name = "vat_number", length = 9)
    private String vatNumber;
}
