package com.tenten.zimparks.dashboard;

import com.tenten.zimparks.creditnote.CreditNoteRepository;
import com.tenten.zimparks.shift.Shift;
import com.tenten.zimparks.shift.ShiftRepository;
import com.tenten.zimparks.transaction.Receipt;
import com.tenten.zimparks.transaction.ReceiptRepository;
import com.tenten.zimparks.transaction.Transaction;
import com.tenten.zimparks.transaction.TransactionRepository;
import com.tenten.zimparks.transaction.TransactionStatus;
import com.tenten.zimparks.user.User;
import com.tenten.zimparks.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ShiftRepository shiftRepo;
    private final TransactionRepository txRepo;
    private final CreditNoteRepository cnRepo;
    private final ReceiptRepository receiptRepo;
    private final UserRepository userRepo;

    // ── Operator dashboard (unchanged) ────────────────────────────────────

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

    // ── Supervisor dashboard (unchanged) ──────────────────────────────────

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


    // ── Admin dashboard ───────────────────────────────────────────────────

    public AdminDashboardDTO getAdminDashboard(int year, int month) {

        String thisMonthToken = monthToken(year, month);
        LocalDate prev = LocalDate.of(year, month, 1).minusMonths(1);
        String prevMonthToken = monthToken(prev.getYear(), prev.getMonthValue());

        // ── Total revenue: PAID txns this month ───────────────────────────
        List<Transaction> thisMonthPaid = txRepo.findByStatusAndTxDateContaining(
                TransactionStatus.PAID, thisMonthToken);

        BigDecimal totalRevenue = thisMonthPaid.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ── Previous month revenue ────────────────────────────────────────
        List<Transaction> lastMonthPaid = txRepo.findByStatusAndTxDateContaining(
                TransactionStatus.PAID, prevMonthToken);

        BigDecimal lastMonthRevenue = lastMonthPaid.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double growthPercent = 0.0;
        if (lastMonthRevenue.compareTo(BigDecimal.ZERO) != 0) {
            growthPercent = totalRevenue.subtract(lastMonthRevenue)
                    .divide(lastMonthRevenue, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
            growthPercent = Math.round(growthPercent * 100.0) / 100.0;
        }

        // ── ATV: average value across all PAID transactions ───────────────
        List<Transaction> allPaid = txRepo.findByStatus(TransactionStatus.PAID);
        BigDecimal atv = BigDecimal.ZERO;
        if (!allPaid.isEmpty()) {
            BigDecimal total = allPaid.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            atv = total.divide(BigDecimal.valueOf(allPaid.size()), 2, RoundingMode.HALF_UP);
        }

        // ── Top products: group by TransactionItem.descr ──────────────────
        Map<String, BigDecimal> revenueByProduct = thisMonthPaid.stream()
                .filter(t -> t.getItemsList() != null && !t.getItemsList().isEmpty())
                .collect(Collectors.groupingBy(
                        t -> t.getItemsList().get(0).getDescr(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        AdminDashboardDTO.TopItem topService = revenueByProduct.entrySet().stream()
                .max(Comparator.comparing(Map.Entry::getValue))
                .map(e -> AdminDashboardDTO.TopItem.builder()
                        .name(e.getKey())
                        .revenue(e.getValue().setScale(2, RoundingMode.HALF_UP))
                        .build())
                .orElse(null);

        // Top 3 services list for donut breakdown
        List<AdminDashboardDTO.TopItem> topServicesList = revenueByProduct.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(3)
                .map(e -> AdminDashboardDTO.TopItem.builder()
                        .name(e.getKey())
                        .revenue(e.getValue().setScale(2, RoundingMode.HALF_UP))
                        .build())
                .collect(Collectors.toList());

        // ── Top location: group by Station.name ───────────────────────────
        Map<String, BigDecimal> revenueByStation = thisMonthPaid.stream()
                .filter(t -> t.getStation() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getStation().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        AdminDashboardDTO.TopItem topLocation = revenueByStation.entrySet().stream()
                .max(Comparator.comparing(Map.Entry::getValue))
                .map(e -> AdminDashboardDTO.TopItem.builder()
                        .name(e.getKey())
                        .revenue(e.getValue().setScale(2, RoundingMode.HALF_UP))
                        .build())
                .orElse(null);

        // ── Top performer: group by operatorName, resolve full name ───────
        Map<String, BigDecimal> revenueByOperator = thisMonthPaid.stream()
                .filter(t -> t.getOperatorName() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getOperatorName,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        AdminDashboardDTO.TopPerformer topPerformer = revenueByOperator.entrySet().stream()
                .max(Comparator.comparing(Map.Entry::getValue))
                .map(e -> {
                    String operatorName = e.getKey();
                    // operatorName stores the full name — look up the actual username
                    User user = userRepo.findByFullName(operatorName)
                            .orElse(null);
                    return AdminDashboardDTO.TopPerformer.builder()
                            .username(user != null ? user.getUsername() : operatorName)
                            .name(user != null ? user.getFullName() : operatorName)
                            .revenue(e.getValue().setScale(2, RoundingMode.HALF_UP))
                            .build();
                })
                .orElse(null);

        return AdminDashboardDTO.builder()
                .totalRevenue(totalRevenue.setScale(2, RoundingMode.HALF_UP))
                .previousMonthRevenue(lastMonthRevenue.setScale(2, RoundingMode.HALF_UP))
                .revenueGrowthPercent(growthPercent)
                .atv(atv)
                .transactionCount(thisMonthPaid.size())
                .topService(topService)
                .topServices(topServicesList)
                .topLocation(topLocation)
                .topPerformer(topPerformer)
                .build();
    }


    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Produces a token like "Mar 2026" that matches inside txDate strings
     * stored by the frontend as "dd MMM yyyy" (e.g. "24 Mar 2026").
     */
    private String monthToken(int year, int month) {
        String abbr = Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH); // "Mar"
        return abbr + " " + year;  // "Mar 2026"
    }

}
