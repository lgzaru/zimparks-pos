package com.tenten.zimparks.bank;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BankService {

    private final BankRepository repo;

    public List<Bank> findAll() { return repo.findAll(); }

    public Bank create(Bank b) { return repo.save(b); }

    public Bank update(String code, Bank b) {
        Bank existing = repo.findById(code)
                .orElseThrow(() -> new RuntimeException("Bank not found: " + code));
        if (b.getName() != null) existing.setName(b.getName());
        if (b.getAccountNumber() != null) existing.setAccountNumber(b.getAccountNumber());
        return repo.save(existing);
    }
}
