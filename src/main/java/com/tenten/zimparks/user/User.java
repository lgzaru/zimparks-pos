package com.tenten.zimparks.user;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tenten.zimparks.station.Station;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity @Table(name = "users")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "station_id")
    @JsonIgnoreProperties({"banks","region"})
    private Station station;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean active = true;
}
