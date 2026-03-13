package com.tenten.zimparks.creditnote;


import com.tenten.zimparks.event.EventStreamController;
import com.tenten.zimparks.shift.NoOpenShiftException;
import com.tenten.zimparks.shift.ShiftRepository;
import com.tenten.zimparks.transaction.Transaction;
import com.tenten.zimparks.transaction.TransactionRepository;
import com.tenten.zimparks.user.Role;
import com.tenten.zimparks.user.User;
import com.tenten.zimparks.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditNoteService {

    private final CreditNoteRepository repo;
    private final TransactionRepository transactionRepo;
    private final UserRepository userRepo;
    private final ShiftRepository shiftRepo;
    private final EventStreamController eventStream;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public List<CreditNote> findAll()                { return repo.findAll(); }
    public List<CreditNote> findByStatus(String s)   { return repo.findByStatus(s.toUpperCase()); }

    public CreditNote findById(String id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Credit note not found: " + id));
    }

    public CreditNote create(CreditNote cn) {
        User user = getCurrentUser();
        var shift = shiftRepo.findTopByOperatorOrderByStartFullDesc(user.getUsername())
                .filter(s -> "Open".equals(s.getStatus()))
                .orElseThrow(() -> new NoOpenShiftException("no open shifts are available"));

        Transaction tx = transactionRepo.findById(cn.getTxRef())
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + cn.getTxRef()));

        cn.setFullValue(tx.getAmount());
        cn.setShiftId(shift.getId());
        
        if (CreditNoteReason.NATIONAL_DISASTER.equals(cn.getReason())) {
            cn.setDeductionPercentage(BigDecimal.ZERO);
            cn.setAmount(tx.getAmount());
        } else if (CreditNoteReason.PERSONAL_ILLNESS.equals(cn.getReason())) {
            if (cn.getNoticeDate() == null) {
                throw new IllegalStateException("Notice date is required for Personal Illness");
            }
            // Parse txDate (assuming format dd MMM yyyy)
            LocalDate bookingDate = LocalDate.parse(tx.getTxDate(), DF);
            long daysBetween = ChronoUnit.DAYS.between(cn.getNoticeDate(), bookingDate);

            if (daysBetween < 30) {
                throw new IllegalStateException("Credit note cannot be processed because the notification requirement (30 days) was not met.");
            }

            cn.setDeductionPercentage(new BigDecimal("15"));
            BigDecimal deduction = tx.getAmount().multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
            cn.setAmount(tx.getAmount().subtract(deduction));
        }

        String id = "CN-" + String.valueOf(System.currentTimeMillis()).substring(9);
        cn.setId(id);
        cn.setStatus("PENDING");
        cn.setNoteDate(LocalDate.now().format(DF));
        cn.setRaisedBy(user.getUsername());
        cn.setRaisedByRole(user.getRole().name());

        CreditNote saved = repo.save(cn);
        eventStream.broadcastCNUpdate();
        return saved;
    }

    public CreditNote approve(String id) {
        User user = getCurrentUser();
        CreditNote cn = repo.findById(id).orElseThrow(() -> new RuntimeException("Credit note not found"));

        if (!"PENDING".equals(cn.getStatus())) {
            throw new IllegalStateException("Credit note is already processed");
        }

        if (user.getUsername().equalsIgnoreCase(cn.getRaisedBy())) {
            throw new IllegalStateException("The person who raised the credit note should not be the one who approves it.");
        }

        Role raiserRole = Role.valueOf(cn.getRaisedByRole());
        Role approverRole = user.getRole();

        if (raiserRole == Role.OPERATOR && approverRole != Role.SUPERVISOR) {
            throw new AccessDeniedException("Operator-raised credit notes must be approved by a Supervisor.");
        } else if (raiserRole == Role.SUPERVISOR && approverRole != Role.ADMIN) {
            throw new AccessDeniedException("Supervisor-raised credit notes must be approved by an Admin.");
        } else if (raiserRole == Role.ADMIN) {
            // The prompt doesn't specify who approves Admin-raised ones, but usually another Admin or just follow the same-person rule
            if (approverRole != Role.ADMIN) {
                throw new AccessDeniedException("Admin-raised credit notes must be approved by an Admin.");
            }
        }

        cn.setStatus("APPROVED");
        cn.setApprovedBy(user.getUsername());

        CreditNote saved = repo.save(cn);
        eventStream.broadcastCNUpdate();
        return saved;
    }

    public CreditNote reject(String id) {
        CreditNote cn = repo.findById(id).orElseThrow(() -> new RuntimeException("Credit note not found"));

        if (!"PENDING".equals(cn.getStatus())) {
            throw new IllegalStateException("Credit note is already processed");
        }

        cn.setStatus("REJECTED");
        CreditNote saved = repo.save(cn);
        eventStream.broadcastCNUpdate();
        return saved;
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
}