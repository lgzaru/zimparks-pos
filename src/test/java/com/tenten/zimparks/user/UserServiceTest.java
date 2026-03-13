package com.tenten.zimparks.user;

import com.tenten.zimparks.bank.Bank;
import com.tenten.zimparks.station.Station;
import com.tenten.zimparks.station.StationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repo;

    @Mock
    private StationRepository stationRepo;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private UserService userService;

    @Test
    void create_shouldFetchFullStationEntity() {
        // Arrange
        Station partialStation = Station.builder().id("ST01").build();
        User inputUser = User.builder()
                .username("testuser")
                .password("password")
                .station(partialStation)
                .build();

        Station fullStation = Station.builder()
                .id("ST01")
                .name("Main Gate")
                .banks(List.of(Bank.builder().code("CBZ").name("CBZ Bank").build()))
                .build();

        when(repo.existsByUsername("testuser")).thenReturn(false);
        when(encoder.encode("password")).thenReturn("encoded_password");
        when(stationRepo.findById("ST01")).thenReturn(Optional.of(fullStation));
        when(repo.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        User result = userService.create(inputUser);

        // Assert
        assertNotNull(result.getStation());
        assertEquals("Main Gate", result.getStation().getName());
        assertFalse(result.getStation().getBanks().isEmpty());
        verify(stationRepo).findById("ST01");
        verify(repo).save(inputUser);
    }

    @Test
    void update_shouldFetchFullStationEntity() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User existingUser = User.builder()
                .id(userId)
                .username("testuser")
                .active(true)
                .build();

        Station partialStation = Station.builder().id("ST02").build();
        User patch = User.builder()
                .username("updateduser")
                .fullName("Updated Name")
                .role(Role.OPERATOR)
                .active(true)
                .station(partialStation)
                .build();

        Station fullStation = Station.builder()
                .id("ST02")
                .name("North Entrance")
                .build();

        when(repo.findByIdAndActiveTrue(userId)).thenReturn(Optional.of(existingUser));
        when(stationRepo.findById("ST02")).thenReturn(Optional.of(fullStation));
        when(repo.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        User result = userService.update(userId, patch);

        // Assert
        assertNotNull(result.getStation());
        assertEquals("North Entrance", result.getStation().getName());
        assertEquals("updateduser", result.getUsername());
        verify(stationRepo).findById("ST02");
    }
}
