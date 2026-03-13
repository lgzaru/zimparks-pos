package com.tenten.zimparks.vat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class VatService {

    private final VatRepository repo;

    public VatSettings get() {
        return repo.findById(1L).orElse(new VatSettings(1L, BigDecimal.valueOf(15.0), BigDecimal.valueOf(15.5)));
    }

    public VatSettings set(BigDecimal zwgRate, BigDecimal otherRate) {
        VatSettings v = get();
        v.setZwgRate(zwgRate);
        v.setOtherRate(otherRate);
        return repo.save(v);
    }
}
