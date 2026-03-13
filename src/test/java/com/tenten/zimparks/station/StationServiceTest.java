package com.tenten.zimparks.station;

import com.tenten.zimparks.bank.Bank;
import com.tenten.zimparks.bank.BankRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StationServiceTest {

    @Mock
    private StationRepository repo;

    @Mock
    private BankRepository bankRepo;

    @InjectMocks
    private StationService stationService;

    @Test
    void addBank_shouldAddBankToStation() {
        // Arrange
        String stationId = "ST01";
        String bankCode = "CBZ";
        Station station = Station.builder().id(stationId).banks(new ArrayList<>()).build();
        Bank bank = Bank.builder().code(bankCode).name("CBZ Bank").build();

        when(repo.findById(stationId)).thenReturn(Optional.of(station));
        when(bankRepo.findById(bankCode)).thenReturn(Optional.of(bank));
        when(repo.save(any(Station.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Station result = stationService.addBank(stationId, bankCode);

        // Assert
        assertTrue(result.getBanks().contains(bank));
        verify(repo).save(station);
    }

    @Test
    void removeBank_shouldRemoveBankFromStation() {
        // Arrange
        String stationId = "ST01";
        String bankCode = "CBZ";
        Bank bank = Bank.builder().code(bankCode).name("CBZ Bank").build();
        List<Bank> banks = new ArrayList<>();
        banks.add(bank);
        Station station = Station.builder().id(stationId).banks(banks).build();

        when(repo.findById(stationId)).thenReturn(Optional.of(station));
        when(repo.save(any(Station.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Station result = stationService.removeBank(stationId, bankCode);

        // Assert
        assertFalse(result.getBanks().stream().anyMatch(b -> b.getCode().equals(bankCode)));
        verify(repo).save(station);
    }
}
