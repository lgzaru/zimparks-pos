package com.tenten.zimparks.vat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class VatService {

    private final VatRepository repo;

    public VatSettings get() {
        return repo.findById(1L).orElseGet(() -> {
            VatSettings v = new VatSettings();
            v.setId(1L);
            v.setZwgRate(BigDecimal.valueOf(15.0));
            v.setOtherRate(BigDecimal.valueOf(15.5));
            return v;
        });
    }

    public VatSettings set(BigDecimal zwgRate, BigDecimal otherRate, String revenueAccount, String vatAccount) {
        VatSettings v = get();
        v.setZwgRate(zwgRate);
        v.setOtherRate(otherRate);
        if (revenueAccount != null) v.setRevenueAccount(revenueAccount);
        if (vatAccount != null) v.setVatAccount(vatAccount);
        return repo.save(v);
    }
}
