package com.tenten.zimparks.currency;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyRepository repo;

    public List<Currency> findAll() {
        return repo.findAll();
    }

    public Optional<Currency> findById(String code) {
        return repo.findById(code);
    }

    public Currency save(Currency currency) {
        return repo.save(currency);
    }

    public void delete(String code) {
        repo.deleteById(code);
    }
}
