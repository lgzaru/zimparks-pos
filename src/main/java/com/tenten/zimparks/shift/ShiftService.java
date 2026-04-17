package com.tenten.zimparks.shift;

import com.tenten.zimparks.cashup.CashupService;
import com.tenten.zimparks.creditnote.CreditNoteRepository;
import com.tenten.zimparks.station.Station;
import com.tenten.zimparks.transaction.Receipt;
import com.tenten.zimparks.transaction.ReceiptRepository;
import com.tenten.zimparks.transaction.TransactionRepository;
import com.tenten.zimparks.transaction.TransactionStatus;
import com.tenten.zimparks.user.Role;
import com.tenten.zimparks.user.User;
import com.tenten.zimparks.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.tenten.zimparks.transaction.PaymentBreakdown;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ShiftRepository       shiftRepo;
    private final TransactionRepository txRepo;
    private final CreditNoteRepository  cnRepo;
    private final ReceiptRepository     receiptRepo;
    private final UserRepository        userRepo;
    private final CashupService         cashupService;

    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public Optional<Shift> getLatest(String username) {
        return shiftRepo.findTopByOperatorOrderByStartFullDesc(username);
    }

    public Shift open(String username) {
        validateShiftAccess(username);
        getLatest(username).ifPresent(s -> {
            if ("Open".equals(s.getStatus()))
                throw new IllegalStateException("Shift already open for " + username);
        });
        String id = "SHF-" + String.valueOf(System.currentTimeMillis()).substring(9);
        LocalDateTime now = LocalDateTime.now();
        Shift s = Shift.builder()
                .id(id).type("NB").status("Open")
                .operator(username)
                .startTime(now.format(TF))
                .startFull(now)
                .build();
        return shiftRepo.save(s);
    }

    public Map<String, Object> close(String username, CloseShiftRequest request) {
        validateShiftAccess(username);
        String currentUsername = getCurrentUsername();
        boolean selfClose = currentUsername.equals(username);

        // When an operator closes their own shift they must declare cashup totals
        if (selfClose) {
            boolean hasLines = request != null
                    && request.getCashupLines() != null
                    && !request.getCashupLines().isEmpty();
            if (!hasLines) {
                throw new IllegalArgumentException(
                        "Cashup declaration is required when closing your own shift.");
            }
        }

        Shift s = getLatest(username)
                .filter(sh -> "Open".equals(sh.getStatus()))
                .orElseThrow(() -> new IllegalStateException("No open shift for " + username));

        if (!cnRepo.findByStatusAndShiftId("PENDING", s.getId()).isEmpty()) {
            throw new IllegalStateException("Cannot close shift with pending credit notes");
        }
        if (!txRepo.findByStatusAndShiftId(TransactionStatus.VOID_PENDING, s.getId()).isEmpty()) {
            throw new IllegalStateException("Cannot close shift with pending void requests");
        }

        LocalDateTime now = LocalDateTime.now();
        s.setStatus("Closed");
        s.setEndTime(now.format(TF));
        s.setEndFull(now);
        s.setClosedBy(currentUsername);
        shiftRepo.save(s);

        // Persist the operator's declared cashup lines
        if (selfClose && request != null && request.getCashupLines() != null) {
            cashupService.submit(s.getId(), currentUsername, request.getCashupLines());
        }

        return getSummary(s);
    }

    public Map<String, Object> getSummary(String username) {
        validateShiftAccess(username);
        Shift s = getLatest(username)
                .orElseThrow(() -> new IllegalStateException("No shift found for " + username));
        return getSummary(s);
    }

    public Object getTransactions(String shiftId) {
        return txRepo.findByShiftId(shiftId);
    }
    
    public List<Shift> getAllByStationId(String stationId) {
        List<User> users = userRepo.findByStationId(stationId);
        List<String> usernames = users.stream().map(User::getUsername).toList();
        return shiftRepo.findByOperatorIn(usernames);
    }

    public List<Map<String, Object>> getActiveShifts() {
        return shiftRepo.findByStatus("Open").stream()
                .map(s -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("username", s.getOperator());
                    map.put("id", s.getId());
                    map.put("status", s.getStatus());
                    map.put("startTime", s.getStartTime());
                    return map;
                })
                .toList();
    }

    private Map<String, Object> getSummary(Shift s) {
        var paid   = txRepo.findByStatusAndShiftId(TransactionStatus.PAID, s.getId());
        var voided = txRepo.findByStatusAndShiftId(TransactionStatus.VOIDED, s.getId());
        var receipts = receiptRepo.findByShiftId(s.getId());

        BigDecimal total = paid.stream()
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> byCurrency = receipts.stream()
                .filter(r -> !TransactionStatus.VOIDED.equals(r.getStatus()))
                .collect(Collectors.groupingBy(
                        Receipt::getOriginalCurrency,
                        Collectors.reducing(BigDecimal.ZERO, Receipt::getOriginalAmount, BigDecimal::add)
                ));

        // Aggregate actual totals by payment type + currency (for cashup comparison)
        Map<String, BigDecimal> byPaymentType = new java.util.LinkedHashMap<>();
        for (var tx : paid) {
            if (tx.getBreakdown() == null) continue;
            for (PaymentBreakdown pb : tx.getBreakdown()) {
                if (pb.getOriginalAmount() == null) continue;
                String key = pb.getType() + "|" + pb.getOriginalCurrency();
                byPaymentType.merge(key, pb.getOriginalAmount(), BigDecimal::add);
            }
        }

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id",             s.getId());
        response.put("operator",       s.getOperator());
        response.put("closedBy",       s.getClosedBy());
        response.put("status",         s.getStatus());
        response.put("start",          s.getStartTime());
        response.put("end",            s.getEndTime());
        response.put("date",           s.getEndFull() != null ? s.getEndFull().format(DF) : s.getStartFull().format(DF));
        response.put("totalTxns",      paid.size());
        response.put("totalBaseAmount", total);
        response.put("byCurrency",     byCurrency);
        response.put("byPaymentType",  byPaymentType);
        response.put("voids",          voided.size());
        response.put("creditNotes",    cnRepo.findByStatusNotAndShiftId("REJECTED", s.getId()).size());

        return response;
    }

    private void validateShiftAccess(String targetUsername) {
        String currentUsername = getCurrentUsername();
        if (currentUsername.equals(targetUsername)) {
            return; // Allowed to manage own shift
        }

        User currentUser = userRepo.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        if (currentUser.getRole() == Role.ADMIN) {
            return; // Admin can manage any shift
        }

        if (currentUser.getRole() == Role.SUPERVISOR) {
            User targetUser = userRepo.findByUsername(targetUsername)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));

            String currentStationId = currentUser.getStation() != null ? currentUser.getStation().getId() : null;
            String targetStationId  = targetUser.getStation()  != null ? targetUser.getStation().getId()  : null;

            if (currentStationId != null && currentStationId.equals(targetStationId)) {
                return; // Supervisor can manage shifts in the same station
            }
            throw new AccessDeniedException("Supervisor can only manage shifts for clerks in their station");
        }

        throw new AccessDeniedException("You do not have permission to manage shifts for " + targetUsername);
    }

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else {
            return principal.toString();
        }
    }
}
