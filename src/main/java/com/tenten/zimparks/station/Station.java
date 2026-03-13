package com.tenten.zimparks.station;

import com.tenten.zimparks.bank.Bank;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "stations")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Station {

    @Id
    @Column(length = 10)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne
    @JoinColumn(name = "region_id")
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(name = "cluster_code", length = 10)
    private Cluster cluster;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "station_banks",
            joinColumns = @JoinColumn(name = "station_id"),
            inverseJoinColumns = @JoinColumn(name = "bank_code")
    )
    private List<Bank> banks = new ArrayList<>();
}