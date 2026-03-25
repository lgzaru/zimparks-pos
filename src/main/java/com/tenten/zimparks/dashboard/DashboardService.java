package com.tenten.zimparks.dashboard;

import com.tenten.zimparks.creditnote.CreditNoteRepository;
import com.tenten.zimparks.shift.Shift;
import com.tenten.zimparks.shift.ShiftRepository;
import com.tenten.zimparks.transaction.Receipt;
import com.tenten.zimparks.transaction.ReceiptRepository;
import com.tenten.zimparks.transaction.TransactionRepository;
import com.tenten.zimparks.transaction.TransactionStatus;
import com.tenten.zimparks.user.User;
import com.tenten.zimparks.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ShiftRepository shiftRepo;
    private final TransactionRepository txRepo;
    private final CreditNoteRepository cnRepo;
    private final ReceiptRepository receiptRepo;
    private final UserRepository userRepo;

    public OperatorDashboardDTO getOperatorDashboard(String username) {
        Shift shift = shiftRepo.findTopByOperatorOrderByStartFullDesc(username)
                .filter(s -> "Open".equals(s.getStatus()))
                .orElseThrow(() -> new IllegalStateException("No open shift found for " + username));

        var paidTransactions = txRepo.findByStatusAndShiftId(TransactionStatus.PAID, shift.getId());
        var voidedTransactions = txRepo.findByStatusAndShiftId(TransactionStatus.VOIDED, shift.getId());
        var creditNotes = cnRepo.findByShiftId(shift.getId());
        var receipts = receiptRepo.findByShiftId(shift.getId());

        BigDecimal totalRevenue = paidTransactions.stream()
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> revenueByCurrency = receipts.stream()
                .filter(r -> !TransactionStatus.VOIDED.equals(r.getStatus()))
                .collect(Collectors.groupingBy(
                        Receipt::getOriginalCurrency,
                        Collectors.reducing(BigDecimal.ZERO, Receipt::getOriginalAmount, BigDecimal::add)
                ));

        return OperatorDashboardDTO.builder()
                .shiftId(shift.getId())
                .status(shift.getStatus())
                .startTime(shift.getStartTime())
                .totalRevenue(totalRevenue)
                .totalVoids(voidedTransactions.size())
                .totalCreditNotes(creditNotes.size())
                .revenueByCurrency(revenueByCurrency)
                .build();
    }

    public SupervisorDashboardDTO getSupervisorDashboard(String supervisorUsername) {
        User supervisor = userRepo.findByUsername(supervisorUsername)
                .orElseThrow(() -> new RuntimeException("Supervisor not found"));

        if (supervisor.getStation() == null) {
            throw new IllegalStateException("Supervisor is not assigned to any station");
        }

        String stationId = supervisor.getStation().getId();
        List<User> stationUsers = userRepo.findByStationId(stationId);
        List<String> usernames = stationUsers.stream().map(User::getUsername).collect(Collectors.toList());

        List<Shift> openShifts = shiftRepo.findByStatusAndOperatorIn("Open", usernames);
        List<String> shiftIds = openShifts.stream().map(Shift::getId).collect(Collectors.toList());

        BigDecimal totalRevenue = BigDecimal.ZERO;
        Map<String, BigDecimal> revenueByCurrency = new java.util.HashMap<>();

        if (!shiftIds.isEmpty()) {
            var paidTransactions = txRepo.findByStatusAndShiftIdIn(TransactionStatus.PAID, shiftIds);
            totalRevenue = paidTransactions.stream()
                    .map(t -> t.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            var receipts = receiptRepo.findByShiftIdIn(shiftIds);
            revenueByCurrency = receipts.stream()
                    .filter(r -> !TransactionStatus.VOIDED.equals(r.getStatus()))
                    .collect(Collectors.groupingBy(
                            Receipt::getOriginalCurrency,
                            Collectors.reducing(BigDecimal.ZERO, Receipt::getOriginalAmount, BigDecimal::add)
                    ));
        }

        long pendingCreditNotes = cnRepo.findByStatusAndRaisedByIn("PENDING", usernames).size();
        long pendingVoids = txRepo.findByStatusAndOperatorNameIn(TransactionStatus.VOID_PENDING, usernames).size();

        return SupervisorDashboardDTO.builder()
                .stationId(stationId)
                .stationName(supervisor.getStation().getName())
                .totalOpenShifts(openShifts.size())
                .totalRevenueFromOpenShifts(totalRevenue)
                .revenueByCurrency(revenueByCurrency)
                .pendingCreditNotes(pendingCreditNotes)
                .pendingVoids(pendingVoids)
                .build();
    }
}
