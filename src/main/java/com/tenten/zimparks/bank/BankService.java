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
}
