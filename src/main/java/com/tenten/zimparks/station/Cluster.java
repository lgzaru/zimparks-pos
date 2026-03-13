package com.tenten.zimparks.station;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Cluster {
    HW("Hwange Cluster", "HW"),
    HE("Harare Cluster", "HE"),
    MT("Matobo Cluster", "MT"),
    MD("Matusadonha Cluster", "MD"),
    MZ("Mid Zambezi Cluster", "MZ"),
    NG("Ngezi Cluster", "NG"),
    NY("Nyanga Cluster", "NY");

    private final String name;
    private final String code;
}
