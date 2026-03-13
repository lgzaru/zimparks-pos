package com.tenten.zimparks.station;

import com.tenten.zimparks.bank.Bank;
import com.tenten.zimparks.bank.BankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StationService {

    private final StationRepository repo;
    private final BankRepository bankRepo;

    public List<Station> findAll()         { return repo.findAll(); }

    public Station create(Station s) {
        if (s.getBanks() != null && !s.getBanks().isEmpty()) {
            s.setBanks(resolveBanksByCode(s.getBanks()));
        }
        return repo.save(s);
    }

    public Station update(String id, Station patch) {
        Station s = repo.findById(id).orElseThrow(() -> new RuntimeException("Station not found"));
        s.setName(patch.getName());
        if (patch.getBanks() != null) {
            s.setBanks(resolveBanksByCode(patch.getBanks()));
        }
        s.setRegion(patch.getRegion());
        s.setCluster(patch.getCluster());
        return repo.save(s);
    }

    public Station addBank(String id, String bankCode) {
        Station s = repo.findById(id).orElseThrow(() -> new RuntimeException("Station not found"));
        Bank bank = bankRepo.findById(bankCode).orElseThrow(() -> new RuntimeException("Bank not found"));
        if (!s.getBanks().contains(bank)) {
            s.getBanks().add(bank);
        }
        return repo.save(s);
    }

    public Station removeBank(String id, String bankCode) {
        Station s = repo.findById(id).orElseThrow(() -> new RuntimeException("Station not found"));
        s.getBanks().removeIf(b -> b.getCode().equals(bankCode));
        return repo.save(s);
    }

    private List<Bank> resolveBanksByCode(List<Bank> banks) {
        List<String> codes = banks.stream().map(Bank::getCode).collect(Collectors.toList());
        return bankRepo.findAllById(codes);
    }

    public void delete(String id)          { repo.deleteById(id); }
}
