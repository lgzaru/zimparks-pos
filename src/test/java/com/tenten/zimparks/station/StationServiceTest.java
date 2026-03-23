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
        String otherBankCode = "FBC";
        Bank bank = Bank.builder().code(bankCode).name("CBZ Bank").build();
        Bank otherBank = Bank.builder().code(otherBankCode).name("FBC Bank").build();
        List<Bank> banks = new ArrayList<>();
        banks.add(bank);
        banks.add(otherBank);
        Station station = Station.builder().id(stationId).banks(banks).build();

        when(repo.findById(stationId)).thenReturn(Optional.of(station));
        when(repo.save(any(Station.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Station result = stationService.removeBank(stationId, bankCode);

        // Assert
        assertFalse(result.getBanks().stream().anyMatch(b -> b.getCode().equals(bankCode)));
        assertTrue(result.getBanks().stream().anyMatch(b -> b.getCode().equals(otherBankCode)));
        verify(repo).save(station);
    }

    @Test
    void create_shouldAllowNoBanks() {
        // Arrange
        Station station = Station.builder().name("Test Station").banks(null).build();
        when(repo.save(any(Station.class))).thenReturn(station);

        // Act
        Station result = stationService.create(station);

        // Assert
        assertNotNull(result);
        assertNull(result.getBanks());
        verify(repo).save(station);
    }

    @Test
    void create_shouldAllowEmptyBanks() {
        // Arrange
        Station station = Station.builder().name("Test Station").banks(new ArrayList<>()).build();
        when(repo.save(any(Station.class))).thenReturn(station);

        // Act
        Station result = stationService.create(station);

        // Assert
        assertNotNull(result);
        assertTrue(result.getBanks().isEmpty());
        verify(repo).save(station);
    }

    @Test
    void update_shouldAllowEmptyBanks() {
        // Arrange
        String stationId = "ST01";
        Station station = Station.builder().id(stationId).banks(new ArrayList<>()).build();
        Station patch = Station.builder().banks(new ArrayList<>()).build();

        when(repo.findById(stationId)).thenReturn(Optional.of(station));
        when(repo.save(any(Station.class))).thenReturn(station);

        // Act
        Station result = stationService.update(stationId, patch);

        // Assert
        assertNotNull(result);
        assertTrue(result.getBanks().isEmpty());
        verify(repo).save(station);
    }

    @Test
    void getClusters_shouldReturnAllClusters() {
        // Act
        List<ClusterDto> result = stationService.getClusters();

        // Assert
        assertEquals(Cluster.values().length, result.size());
        assertEquals(Cluster.HW.getName(), result.get(0).getName());
        assertEquals(Cluster.HW.getCode(), result.get(0).getCode());
    }
}
