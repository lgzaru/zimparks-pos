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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_permissions",
            joinColumns = @JoinColumn(name = "user_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uq_user_permissions",
                    columnNames = {"user_id", "permission"}
            )
    )
    @Column(name = "permission")
    @Enumerated(EnumType.STRING)
    private java.util.Set<Permission> permissions;

    @ManyToOne
    @JoinColumn(name = "station_id")
    @JsonIgnoreProperties({"banks","region"})
    private Station station;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "current_token", length = 1000)
    private String currentToken;

    @Column(name = "cell_phone", unique = true, length = 20)
    private String cellPhone;

    @Column(name = "reset_otp", length = 6)
    private String resetOtp;

    @Column(name = "reset_otp_expiry")
    private java.time.LocalDateTime resetOtpExpiry;
}
