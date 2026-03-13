package com.tenten.zimparks.creditnote;

import com.tenten.zimparks.event.EventStreamController;
import com.tenten.zimparks.transaction.Transaction;
import com.tenten.zimparks.transaction.TransactionRepository;
import com.tenten.zimparks.user.Role;
import com.tenten.zimparks.user.User;
import com.tenten.zimparks.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreditNoteServiceTest {

    @Mock
    private CreditNoteRepository repo;
    @Mock
    private TransactionRepository transactionRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;
    @Mock
    private UserDetails userDetails;
    @Mock
    private EventStreamController eventStream;
    @Mock
    private com.tenten.zimparks.shift.ShiftRepository shiftRepo;

    @InjectMocks
    private CreditNoteService service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    private void mockUser(String username, Role role) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(username);
        User user = User.builder().username(username).role(role).build();
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));
    }

    @Test
    void testCreateNationalDisaster() {
        mockUser("operator", Role.OPERATOR);
        com.tenten.zimparks.shift.Shift shift = com.tenten.zimparks.shift.Shift.builder().id("SHF1").status("Open").build();
        when(shiftRepo.findTopByOperatorOrderByStartFullDesc("operator")).thenReturn(Optional.of(shift));
        
        Transaction tx = Transaction.builder().ref("TX1").amount(new BigDecimal("100.00")).txDate("10 Mar 2026").build();
        when(transactionRepo.findById("TX1")).thenReturn(Optional.of(tx));
        when(repo.save(any())).thenAnswer(i -> i.getArguments()[0]);

        CreditNote cn = new CreditNote();
        cn.setTxRef("TX1");
        cn.setReason(CreditNoteReason.NATIONAL_DISASTER);

        CreditNote result = service.create(cn);

        verify(eventStream).broadcastCNUpdate();
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        assertEquals(BigDecimal.ZERO, result.getDeductionPercentage());
        assertEquals("PENDING", result.getStatus());
        assertEquals("operator", result.getRaisedBy());
    }

    @Test
    void testCreatePersonalIllnessSuccess() {
        mockUser("operator", Role.OPERATOR);
        com.tenten.zimparks.shift.Shift shift = com.tenten.zimparks.shift.Shift.builder().id("SHF1").status("Open").build();
        when(shiftRepo.findTopByOperatorOrderByStartFullDesc("operator")).thenReturn(Optional.of(shift));
        
        // Booking date is 10 Apr 2026, notice date is 10 Mar 2026 (31 days before)
        Transaction tx = Transaction.builder().ref("TX1").amount(new BigDecimal("100.00")).txDate("10 Apr 2026").build();
        when(transactionRepo.findById("TX1")).thenReturn(Optional.of(tx));
        when(repo.save(any())).thenAnswer(i -> i.getArguments()[0]);

        CreditNote cn = new CreditNote();
        cn.setTxRef("TX1");
        cn.setReason(CreditNoteReason.PERSONAL_ILLNESS);
        cn.setNoticeDate(LocalDate.of(2026, 3, 10));

        CreditNote result = service.create(cn);

        verify(eventStream).broadcastCNUpdate();
        assertEquals(new BigDecimal("85.00"), result.getAmount());
        assertEquals(new BigDecimal("15"), result.getDeductionPercentage());
    }

    @Test
    void testCreatePersonalIllnessFailure() {
        mockUser("operator", Role.OPERATOR);
        com.tenten.zimparks.shift.Shift shift = com.tenten.zimparks.shift.Shift.builder().id("SHF1").status("Open").build();
        when(shiftRepo.findTopByOperatorOrderByStartFullDesc("operator")).thenReturn(Optional.of(shift));
        
        // Booking date is 20 Mar 2026, notice date is 10 Mar 2026 (10 days before)
        Transaction tx = Transaction.builder().ref("TX1").amount(new BigDecimal("100.00")).txDate("20 Mar 2026").build();
        when(transactionRepo.findById("TX1")).thenReturn(Optional.of(tx));

        CreditNote cn = new CreditNote();
        cn.setTxRef("TX1");
        cn.setReason(CreditNoteReason.PERSONAL_ILLNESS);
        cn.setNoticeDate(LocalDate.of(2026, 3, 10));

        Exception exception = assertThrows(RuntimeException.class, () -> service.create(cn));
        // System.out.println("[DEBUG_LOG] Error message: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("notification requirement"), "Expected message to contain 'notification requirement' but was: " + exception.getMessage());
    }

    @Test
    void testApproveOperatorBySupervisor() {
        mockUser("supervisor", Role.SUPERVISOR);
        CreditNote cn = CreditNote.builder()
                .id("CN1")
                .status("PENDING")
                .raisedBy("operator")
                .raisedByRole("OPERATOR")
                .build();
        when(repo.findById("CN1")).thenReturn(Optional.of(cn));
        when(repo.save(any())).thenAnswer(i -> i.getArguments()[0]);

        CreditNote result = service.approve("CN1");

        assertEquals("APPROVED", result.getStatus());
        assertEquals("supervisor", result.getApprovedBy());
        verify(eventStream).broadcastCNUpdate();
    }

    @Test
    void testApproveSupervisorByAdmin() {
        mockUser("admin", Role.ADMIN);
        CreditNote cn = CreditNote.builder()
                .id("CN1")
                .status("PENDING")
                .raisedBy("supervisor")
                .raisedByRole("SUPERVISOR")
                .build();
        when(repo.findById("CN1")).thenReturn(Optional.of(cn));
        when(repo.save(any())).thenAnswer(i -> i.getArguments()[0]);

        CreditNote result = service.approve("CN1");

        verify(eventStream).broadcastCNUpdate();
        assertEquals("APPROVED", result.getStatus());
        assertEquals("admin", result.getApprovedBy());
    }

    @Test
    void testApproveSamePersonFailure() {
        mockUser("supervisor", Role.SUPERVISOR);
        CreditNote cn = CreditNote.builder()
                .id("CN1")
                .status("PENDING")
                .raisedBy("supervisor")
                .raisedByRole("SUPERVISOR")
                .build();
        when(repo.findById("CN1")).thenReturn(Optional.of(cn));

        Exception exception = assertThrows(RuntimeException.class, () -> service.approve("CN1"));
        assertTrue(exception.getMessage().contains("should not be the one who approves it"));
    }

    @Test
    void testApproveOperatorByOperatorFailure() {
        mockUser("operator2", Role.OPERATOR);
        CreditNote cn = CreditNote.builder()
                .id("CN1")
                .status("PENDING")
                .raisedBy("operator1")
                .raisedByRole("OPERATOR")
                .build();
        when(repo.findById("CN1")).thenReturn(Optional.of(cn));

        Exception exception = assertThrows(RuntimeException.class, () -> service.approve("CN1"));
        assertTrue(exception.getMessage().contains("must be approved by a Supervisor"));
    }

    @Test
    void testRejectCreditNote() {
        CreditNote cn = CreditNote.builder()
                .id("CN1")
                .status("PENDING")
                .build();
        when(repo.findById("CN1")).thenReturn(Optional.of(cn));
        when(repo.save(any())).thenAnswer(i -> i.getArguments()[0]);

        CreditNote result = service.reject("CN1");

        assertEquals("REJECTED", result.getStatus());
        verify(eventStream).broadcastCNUpdate();
    }
}
