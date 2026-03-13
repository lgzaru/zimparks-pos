package com.tenten.zimparks.transaction;


import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "transaction_vehicles")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionVehicle {

    @Id @Column(name = "tx_ref", length = 30)
    private String txRef;

    @Column(length = 20) private String plate;
    @Column(length = 50) private String make;
    @Column(length = 50) private String model;
    @Column(length = 50) private String colour;
}
