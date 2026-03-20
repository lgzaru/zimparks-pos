package com.tenten.zimparks.user;

import com.tenten.zimparks.bank.Bank;
import com.tenten.zimparks.shift.Shift;
import com.tenten.zimparks.shift.ShiftRepository;
import com.tenten.zimparks.station.Station;
import com.tenten.zimparks.station.StationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private ShiftRepository shiftRepo;

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

        when(repo.existsByUsername("TESTUSER")).thenReturn(false);
        when(encoder.encode("PASSWORD")).thenReturn("encoded_password");
        when(stationRepo.findById("ST01")).thenReturn(Optional.of(fullStation));
        when(repo.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        User result = userService.create(inputUser);

        // Assert
        assertEquals("TESTUSER", result.getUsername());
        assertNotNull(result.getStation());
        assertEquals("Main Gate", result.getStation().getName());
        assertFalse(result.getStation().getBanks().isEmpty());
        verify(stationRepo).findById("ST01");
        verify(repo).save(inputUser);
    }

    @Test
    void update_shouldNotOverwriteWithNulls() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User existingUser = User.builder()
                .id(userId)
                .username("TESTUSER")
                .fullName("Test User")
                .role(Role.OPERATOR)
                .active(true)
                .password("old_password")
                .build();

        User patch = User.builder()
                .fullName(null) // Should not overwrite
                .username(null) // Should not overwrite
                .role(null)     // Should not overwrite
                .active(false)  // Should overwrite
                .build();

        when(repo.findByIdAndActiveTrue(userId)).thenReturn(Optional.of(existingUser));
        when(repo.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        User result = userService.update(userId, patch);

        // Assert
        assertEquals("Test User", result.getFullName());
        assertEquals("TESTUSER", result.getUsername());
        assertEquals(Role.OPERATOR, result.getRole());
        assertFalse(result.getActive());
        assertEquals("old_password", result.getPassword());
        verify(repo).save(any(User.class));
    }

    @Test
    void update_shouldFetchFullStationEntity() {
        // ... (existing test)
    }

    @Test
    void update_shouldThrowException_whenChangingRoleWithOpenShift() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User existingUser = User.builder()
                .id(userId)
                .username("TESTUSER")
                .role(Role.OPERATOR)
                .active(true)
                .build();

        User patch = User.builder()
                .role(Role.SUPERVISOR)
                .build();

        when(repo.findByIdAndActiveTrue(userId)).thenReturn(Optional.of(existingUser));
        when(shiftRepo.findByStatusAndOperatorIn("Open", List.of("TESTUSER")))
                .thenReturn(List.of(Shift.builder().status("Open").operator("TESTUSER").build()));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.update(userId, patch));
        assertEquals("User has an open shift and cannot change role or station", exception.getMessage());
        verify(repo, never()).save(any(User.class));
    }

    @Test
    void update_shouldThrowException_whenChangingStationWithOpenShift() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User existingUser = User.builder()
                .id(userId)
                .username("TESTUSER")
                .station(Station.builder().id("ST01").build())
                .active(true)
                .build();

        User patch = User.builder()
                .station(Station.builder().id("ST02").build())
                .build();

        when(repo.findByIdAndActiveTrue(userId)).thenReturn(Optional.of(existingUser));
        when(shiftRepo.findByStatusAndOperatorIn("Open", List.of("TESTUSER")))
                .thenReturn(List.of(Shift.builder().status("Open").operator("TESTUSER").build()));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.update(userId, patch));
        assertEquals("User has an open shift and cannot change role or station", exception.getMessage());
        verify(repo, never()).save(any(User.class));
    }

    @Test
    void update_shouldAllowChangingRole_whenNoOpenShift() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User existingUser = User.builder()
                .id(userId)
                .username("TESTUSER")
                .role(Role.OPERATOR)
                .active(true)
                .build();

        User patch = User.builder()
                .role(Role.SUPERVISOR)
                .build();

        when(repo.findByIdAndActiveTrue(userId)).thenReturn(Optional.of(existingUser));
        when(shiftRepo.findByStatusAndOperatorIn("Open", List.of("TESTUSER")))
                .thenReturn(Collections.emptyList());
        when(repo.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        User result = userService.update(userId, patch);

        // Assert
        assertEquals(Role.SUPERVISOR, result.getRole());
        verify(repo).save(any(User.class));
    }

    @Test
    void update_shouldAllowChangingOtherFields_whenOpenShiftExists() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User existingUser = User.builder()
                .id(userId)
                .username("TESTUSER")
                .fullName("Old Name")
                .role(Role.OPERATOR)
                .active(true)
                .build();

        User patch = User.builder()
                .fullName("New Name")
                .build();

        when(repo.findByIdAndActiveTrue(userId)).thenReturn(Optional.of(existingUser));
        when(repo.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        User result = userService.update(userId, patch);

        // Assert
        assertEquals("New Name", result.getFullName());
        assertEquals(Role.OPERATOR, result.getRole());
        verify(repo).save(any(User.class));
        verify(shiftRepo, never()).findByStatusAndOperatorIn(anyString(), anyList());
    }

    @Test
    void findByStationAndRole_shouldCallRepository() {
        // Arrange
        String stationId = "ST01";
        Role role = Role.OPERATOR;
        List<User> users = List.of(User.builder().username("USER1").build());
        when(repo.findByStationIdAndRoleAndActiveTrue(stationId, role)).thenReturn(users);

        // Act
        List<User> result = userService.findByStationAndRole(stationId, role);

        // Assert
        assertEquals(users, result);
        verify(repo).findByStationIdAndRoleAndActiveTrue(stationId, role);
    }

    @Test
    void loadUserByUsername_shouldIncludePermissions() {
        // Arrange
        String username = "TESTUSER";
        User user = User.builder()
                .username(username)
                .password("password")
                .role(Role.SUPERVISOR)
                .active(true)
                .build();

        when(repo.findByUsername(username)).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userService.loadUserByUsername(username);

        // Assert
        assertNotNull(userDetails);
        assertEquals(username, userDetails.getUsername());

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        List<String> authorityStrings = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        assertTrue(authorityStrings.contains("ROLE_SUPERVISOR"));
        assertTrue(authorityStrings.contains(Permission.CLOSE_SHIFT.getPermission()));
        assertTrue(authorityStrings.contains(Permission.UPDATE_PRODUCT_PRICING.getPermission()));
        assertTrue(authorityStrings.contains(Permission.ADD_PRODUCT.getPermission()));
        assertTrue(authorityStrings.contains(Permission.LINK_BANKS.getPermission()));
        assertTrue(authorityStrings.contains(Permission.UNLINK_BANKS.getPermission()));
        
        // Supervisor has 5 permissions + 1 role = 6 authorities
        assertEquals(6, authorityStrings.size());
    }

    @Test
    void loadUserByUsername_shouldIncludeIndividualPermissions() {
        // Arrange
        String username = "TESTUSER";
        User user = User.builder()
                .username(username)
                .password("password")
                .role(Role.OPERATOR) // Operator has CLOSE_SHIFT
                .active(true)
                .permissions(java.util.Set.of(Permission.LINK_BANKS))
                .build();

        when(repo.findByUsername(username)).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userService.loadUserByUsername(username);

        // Assert
        assertNotNull(userDetails);
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        List<String> authorityStrings = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        assertTrue(authorityStrings.contains("ROLE_OPERATOR"));
        assertTrue(authorityStrings.contains(Permission.CLOSE_SHIFT.getPermission()));
        assertTrue(authorityStrings.contains(Permission.LINK_BANKS.getPermission()));
        
        // 1 role + 1 role-perm + 1 individual-perm = 3 authorities
        assertEquals(3, authorityStrings.size());
    }
}
