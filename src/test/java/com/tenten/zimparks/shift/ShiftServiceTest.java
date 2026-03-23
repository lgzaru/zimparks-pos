package com.tenten.zimparks.shift;

import com.tenten.zimparks.creditnote.CreditNoteRepository;
import com.tenten.zimparks.transaction.Receipt;
import com.tenten.zimparks.transaction.ReceiptRepository;
import com.tenten.zimparks.transaction.TransactionRepository;
import com.tenten.zimparks.station.Station;
import com.tenten.zimparks.user.Role;
import com.tenten.zimparks.user.User;
import com.tenten.zimparks.user.UserRepository;
import com.tenten.zimparks.transaction.PaymentBreakdown;
import com.tenten.zimparks.transaction.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ShiftServiceTest {

    @Mock
    private ShiftRepository shiftRepo;
    @Mock
    private TransactionRepository txRepo;
    @Mock
    private CreditNoteRepository cnRepo;
    @Mock
    private ReceiptRepository receiptRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private ShiftService shiftService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @Test
    void supervisor_should_not_open_shift_for_clerk_in_different_station() {
        // Arrange
        String supervisorUsername = "supervisor";
        String clerkUsername = "clerk";
        
        Station station1 = Station.builder().id("ST01").name("Station 1").build();
        Station station2 = Station.builder().id("ST02").name("Station 2").build();

        User supervisor = User.builder()
                .username(supervisorUsername)
                .role(Role.SUPERVISOR)
                .station(station1)
                .build();
        
        User clerk = User.builder()
                .username(clerkUsername)
                .role(Role.OPERATOR)
                .station(station2)
                .build();

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(supervisorUsername);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        
        when(userRepo.findByUsername(supervisorUsername)).thenReturn(Optional.of(supervisor));
        when(userRepo.findByUsername(clerkUsername)).thenReturn(Optional.of(clerk));

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> shiftService.open(clerkUsername));
    }

    @Test
    void supervisor_should_open_shift_for_clerk_in_same_station() {
        // Arrange
        String supervisorUsername = "supervisor";
        String clerkUsername = "clerk";
        
        Station station1 = Station.builder().id("ST01").name("Station 1").build();

        User supervisor = User.builder()
                .username(supervisorUsername)
                .role(Role.SUPERVISOR)
                .station(station1)
                .build();
        
        User clerk = User.builder()
                .username(clerkUsername)
                .role(Role.OPERATOR)
                .station(station1)
                .build();

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(supervisorUsername);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        
        when(userRepo.findByUsername(supervisorUsername)).thenReturn(Optional.of(supervisor));
        when(userRepo.findByUsername(clerkUsername)).thenReturn(Optional.of(clerk));
        
        when(shiftRepo.findTopByOperatorOrderByStartFullDesc(clerkUsername)).thenReturn(Optional.empty());

        // Act (should not throw AccessDeniedException)
        shiftService.open(clerkUsername);
    }
    @Test
    void shift_should_be_closed_with_actor_username() {
        // Arrange
        String supervisorUsername = "supervisor";
        String clerkUsername = "clerk";

        Station station1 = Station.builder().id("ST01").name("Station 1").build();

        User supervisor = User.builder()
                .username(supervisorUsername)
                .role(Role.SUPERVISOR)
                .station(station1)
                .build();

        User clerk = User.builder()
                .username(clerkUsername)
                .role(Role.OPERATOR)
                .station(station1)
                .build();

        Shift openShift = Shift.builder()
                .id("SHF-123")
                .operator(clerkUsername)
                .status("Open")
                .startTime("08:00 AM")
                .build();

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(supervisorUsername);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(userRepo.findByUsername(supervisorUsername)).thenReturn(Optional.of(supervisor));
        when(userRepo.findByUsername(clerkUsername)).thenReturn(Optional.of(clerk));
        when(shiftRepo.findTopByOperatorOrderByStartFullDesc(clerkUsername)).thenReturn(Optional.of(openShift));
        when(cnRepo.findByStatusAndShiftId("PENDING", openShift.getId())).thenReturn(Collections.emptyList());
        when(txRepo.findByStatusAndShiftId("PENDING_VOID", openShift.getId())).thenReturn(Collections.emptyList());
        when(txRepo.findByStatusAndShiftId("PAID", openShift.getId())).thenReturn(Collections.emptyList());
        when(txRepo.findByStatusAndShiftId("VOIDED", openShift.getId())).thenReturn(Collections.emptyList());
        when(receiptRepo.findByShiftId(openShift.getId())).thenReturn(Collections.emptyList());
        when(cnRepo.findByStatusNotAndShiftId("REJECTED", openShift.getId())).thenReturn(Collections.emptyList());
        when(shiftRepo.save(any(Shift.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Map<String, Object> result = shiftService.close(clerkUsername);

        // Assert
        ArgumentCaptor<Shift> shiftCaptor = ArgumentCaptor.forClass(Shift.class);
        verify(shiftRepo).save(shiftCaptor.capture());
        
        Shift savedShift = shiftCaptor.getValue();
        assertEquals("Closed", savedShift.getStatus());
        assertEquals(supervisorUsername, savedShift.getClosedBy());
        assertEquals(supervisorUsername, result.get("closedBy"));
    }

    @Test
    void close_should_throw_IllegalStateException_when_no_open_shift() {
        // Arrange
        String username = "operator1";
        
        User operator = User.builder()
                .username(username)
                .role(Role.OPERATOR)
                .build();

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(username);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        
        when(shiftRepo.findTopByOperatorOrderByStartFullDesc(username)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> shiftService.close(username));
        assertEquals("No open shift for " + username, exception.getMessage());
    }

    @Test
    void close_should_throw_IllegalStateException_when_pending_credit_notes_exist() {
        // Arrange
        String username = "operator1";
        String shiftId = "SHF-123";
        Shift openShift = Shift.builder().id(shiftId).operator(username).status("Open").build();

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(username);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(shiftRepo.findTopByOperatorOrderByStartFullDesc(username)).thenReturn(Optional.of(openShift));
        
        // Simulating pending credit notes
        when(cnRepo.findByStatusAndShiftId("PENDING", shiftId)).thenReturn(java.util.List.of(new com.tenten.zimparks.creditnote.CreditNote()));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> shiftService.close(username));
        assertEquals("Cannot close shift with pending credit notes", exception.getMessage());
    }

    @Test
    void close_should_throw_IllegalStateException_when_pending_voids_exist() {
        // Arrange
        String username = "operator1";
        String shiftId = "SHF-123";
        Shift openShift = Shift.builder().id(shiftId).operator(username).status("Open").build();

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(username);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(shiftRepo.findTopByOperatorOrderByStartFullDesc(username)).thenReturn(Optional.of(openShift));
        
        // Simulating no pending credit notes
        when(cnRepo.findByStatusAndShiftId("PENDING", shiftId)).thenReturn(Collections.emptyList());
        // Simulating pending void requests
        when(txRepo.findByStatusAndShiftId("PENDING_VOID", shiftId)).thenReturn(java.util.List.of(new Transaction()));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> shiftService.close(username));
        assertEquals("Cannot close shift with pending void requests", exception.getMessage());
    }

    @Test
    void getSummary_should_return_totals_by_currency() {
        // Arrange
        String username = "operator1";
        String shiftId = "SHF-123";

        Shift shift = Shift.builder()
                .id(shiftId)
                .operator(username)
                .status("Closed")
                .startTime("08:00 AM")
                .startFull(java.time.LocalDateTime.now().minusHours(2))
                .endTime("10:00 AM")
                .endFull(java.time.LocalDateTime.now())
                .closedBy(username)
                .build();

        Receipt r1 = Receipt.builder()
                .txRef("TX1")
                .originalCurrency("USD")
                .originalAmount(new java.math.BigDecimal("15.00"))
                .status("PAID")
                .build();
        Receipt r2 = Receipt.builder()
                .txRef("TX2")
                .originalCurrency("ZWG")
                .originalAmount(new java.math.BigDecimal("100.00"))
                .status("PAID")
                .build();

        Transaction t1 = Transaction.builder()
                .ref("TX1")
                .amount(new java.math.BigDecimal("15.00"))
                .status("PAID")
                .breakdown(Collections.emptyList())
                .build();
        Transaction t2 = Transaction.builder()
                .ref("TX2")
                .amount(new java.math.BigDecimal("100.00"))
                .status("PAID")
                .breakdown(Collections.emptyList())
                .build();

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(username);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(shiftRepo.findTopByOperatorOrderByStartFullDesc(username)).thenReturn(Optional.of(shift));
        when(txRepo.findByStatusAndShiftId("PAID", shiftId)).thenReturn(java.util.List.of(t1, t2));
        when(txRepo.findByStatusAndShiftId("VOIDED", shiftId)).thenReturn(Collections.emptyList());
        when(receiptRepo.findByShiftId(shiftId)).thenReturn(java.util.List.of(r1, r2));
        when(cnRepo.findByStatusNotAndShiftId("REJECTED", shiftId)).thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> summary = shiftService.getSummary(username);

        // Assert
        assertEquals(shiftId, summary.get("id"));
        assertEquals(2, summary.get("totalTxns"));
        assertEquals(new java.math.BigDecimal("115.00"), summary.get("totalBaseAmount"));

        @SuppressWarnings("unchecked")
        Map<String, java.math.BigDecimal> byCurrency = (Map<String, java.math.BigDecimal>) summary.get("byCurrency");
        assertEquals(new java.math.BigDecimal("15.00"), byCurrency.get("USD"));
        assertEquals(new java.math.BigDecimal("100.00"), byCurrency.get("ZWG"));
    }

    @Test
    void getTransactions_should_return_transactions_by_shift_id() {
        // Arrange
        String shiftId = "SHF-123";
        Transaction tx = Transaction.builder().ref("TX-1").shiftId(shiftId).build();
        when(txRepo.findByShiftId(shiftId)).thenReturn(java.util.List.of(tx));

        // Use Lenient for security context if not used
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);

        // Act
        @SuppressWarnings("unchecked")
        java.util.List<Transaction> result = (java.util.List<Transaction>) shiftService.getTransactions(shiftId);

        // Assert
        assertEquals(1, result.size());
        assertEquals("TX-1", result.get(0).getRef());
        verify(txRepo).findByShiftId(shiftId);
    }

    @Test
    void getAllByStationId_should_return_shifts_for_all_users_in_station() {
        // Arrange
        String stationId = "STN-1";
        User user1 = User.builder().username("user1").build();
        User user2 = User.builder().username("user2").build();
        when(userRepo.findByStationId(stationId)).thenReturn(java.util.List.of(user1, user2));

        Shift shift1 = Shift.builder().id("SHF-1").operator("user1").build();
        Shift shift2 = Shift.builder().id("SHF-2").operator("user2").build();
        when(shiftRepo.findByOperatorIn(java.util.List.of("user1", "user2"))).thenReturn(java.util.List.of(shift1, shift2));

        // Act
        java.util.List<Shift> result = shiftService.getAllByStationId(stationId);

        // Assert
        assertEquals(2, result.size());
        verify(userRepo).findByStationId(stationId);
        verify(shiftRepo).findByOperatorIn(java.util.List.of("user1", "user2"));
    }

    @Test
    void getActiveShifts_should_return_mapped_open_shifts() {
        Shift s1 = Shift.builder().id("SHF-001").operator("op1").status("Open").startTime("08:00").build();
        Shift s2 = Shift.builder().id("SHF-002").operator("op2").status("Open").startTime("09:00").build();
        when(shiftRepo.findByStatus("Open")).thenReturn(java.util.List.of(s1, s2));

        java.util.List<Map<String, Object>> active = shiftService.getActiveShifts();

        assertEquals(2, active.size());
        assertEquals("op1", active.get(0).get("username"));
        assertEquals("SHF-001", active.get(0).get("id"));
        assertEquals("Open", active.get(0).get("status"));
        assertEquals("08:00", active.get(0).get("startTime"));

        assertEquals("op2", active.get(1).get("username"));
        assertEquals("SHF-002", active.get(1).get("id"));
    }
}
