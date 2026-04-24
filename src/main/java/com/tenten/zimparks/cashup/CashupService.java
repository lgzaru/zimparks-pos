package com.tenten.zimparks.cashup;

import com.tenten.zimparks.event.EventStreamController;
import com.tenten.zimparks.shift.Shift;
import com.tenten.zimparks.shift.ShiftRepository;
import com.tenten.zimparks.transaction.PaymentBreakdown;
import com.tenten.zimparks.transaction.Transaction;
import com.tenten.zimparks.transaction.TransactionRepository;
import com.tenten.zimparks.transaction.TransactionStatus;
import com.tenten.zimparks.user.Role;
import com.tenten.zimparks.user.User;
import com.tenten.zimparks.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CashupService {

    private final CashupSubmissionRepository cashupRepo;
    private final TransactionRepository      txRepo;
    private final UserRepository             userRepo;
    private final ShiftRepository            shiftRepo;
    private final EventStreamController      eventStream;

    // ── Submit cashup (called when operator closes own shift) ─────────────────

    public CashupSubmission submit(String shiftId, String submittedBy, List<CashupLineDto> lineDtos) {
        Optional<CashupSubmission> existing = cashupRepo.findByShiftId(shiftId);

        if (existing.isPresent()) {
            CashupSubmission prev = existing.get();
            // Only allow resubmission if the previous attempt was rejected
            if (!"REJECTED".equals(prev.getStatus())) {
                throw new IllegalStateException(
                    "Cashup already submitted for shift " + shiftId +
                    " (status: " + prev.getStatus() + "). " +
                    "Resubmission is only allowed after a rejection.");
            }
            // Delete the rejected record so a clean one can be saved
            cashupRepo.delete(prev);
        }

        List<CashupLine> lines = lineDtos == null ? List.of() :
            lineDtos.stream()
                .filter(l -> l.getSubmittedAmount() != null
                          && l.getSubmittedAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(l -> CashupLine.builder()
                        .type(l.getType())
                        .currency(l.getCurrency())
                        .submittedAmount(l.getSubmittedAmount())
                        .build())
                .collect(Collectors.toList());

        CashupSubmission submission = CashupSubmission.builder()
                .shiftId(shiftId)
                .submittedBy(submittedBy)
                .submittedAt(LocalDateTime.now())
                .status("PENDING_REVIEW")
                .lines(lines)
                .build();

        CashupSubmission saved = cashupRepo.save(submission);
        // Notify only supervisors at the operator's station
        String stationId = userRepo.findByUsername(submittedBy)
                .map(u -> u.getStation() != null ? u.getStation().getId() : null)
                .orElse(null);
        eventStream.broadcastCashupUpdate(stationId);
        return saved;
    }

    // ── Get actuals for a shift (for non-blind operator pre-fill or supervisor review) ──

    public Map<String, BigDecimal> getActuals(String shiftId) {
        String currentUsername = getCurrentUsername();
        Shift shift = shiftRepo.findById(shiftId)
                .orElseThrow(() -> new NoSuchElementException("Shift not found: " + shiftId));

        // Only the operator themselves, supervisors in same station, or admins
        if (!canAccessShift(currentUsername, shift)) {
            throw new AccessDeniedException("No permission to view actuals for shift " + shiftId);
        }

        List<Transaction> paidTxns = txRepo.findByStatusAndShiftId(TransactionStatus.PAID, shiftId);
        return computeActuals(paidTxns);
    }

    // ── Get cashup with submitted vs actual comparison ────────────────────────

    public Map<String, Object> getCashupWithActuals(Long cashupId) {
        String currentUsername = getCurrentUsername();
        CashupSubmission submission = cashupRepo.findById(cashupId)
                .orElseThrow(() -> new NoSuchElementException("Cashup not found: " + cashupId));

        User currentUser = userRepo.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (currentUser.getRole() != Role.SUPERVISOR
                && currentUser.getRole() != Role.ADMIN
                && !currentUsername.equals(submission.getSubmittedBy())) {
            throw new AccessDeniedException("No permission to view this cashup");
        }

        List<Transaction> paidTxns = txRepo.findByStatusAndShiftId(TransactionStatus.PAID, submission.getShiftId());
        Map<String, BigDecimal> actuals = computeActuals(paidTxns);

        // Collect all unique (type|currency) keys from both submitted and actual
        Set<String> allKeys = new LinkedHashSet<>();
        if (submission.getLines() != null) {
            submission.getLines().forEach(l -> allKeys.add(l.getType() + "|" + l.getCurrency()));
        }
        allKeys.addAll(actuals.keySet());

        List<Map<String, Object>> comparison = new ArrayList<>();
        for (String key : allKeys) {
            String[] parts  = key.split("\\|", 2);
            String type     = parts[0];
            String currency = parts.length > 1 ? parts[1] : "USD";

            BigDecimal submitted = submission.getLines() == null ? BigDecimal.ZERO :
                submission.getLines().stream()
                    .filter(l -> l.getType().equals(type) && l.getCurrency().equals(currency))
                    .map(CashupLine::getSubmittedAmount)
                    .findFirst().orElse(BigDecimal.ZERO);

            BigDecimal actual = actuals.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal shortfall = actual.subtract(submitted); // positive = shortage

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("type",      type);
            row.put("currency",  currency);
            row.put("submitted", submitted);
            row.put("actual",    actual);
            row.put("short",     shortfall);
            comparison.add(row);
        }

        boolean hasShorts = comparison.stream()
                .anyMatch(r -> ((BigDecimal) r.get("short")).compareTo(BigDecimal.ZERO) > 0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id",            submission.getId());
        result.put("shiftId",       submission.getShiftId());
        result.put("submittedBy",   submission.getSubmittedBy());
        result.put("submittedAt",   submission.getSubmittedAt());
        result.put("status",        submission.getStatus());
        result.put("reviewedBy",    submission.getReviewedBy());
        result.put("reviewedAt",    submission.getReviewedAt());
        result.put("reviewerNotes", submission.getReviewerNotes());
        result.put("comparison",    comparison);
        result.put("hasShorts",     hasShorts);
        return result;
    }

    // ── Get cashup for a specific shift (lightweight, no actuals) ─────────────

    public Optional<CashupSubmission> getByShiftId(String shiftId) {
        return cashupRepo.findByShiftId(shiftId);
    }

    // ── List pending cashups for a supervisor's station ───────────────────────

    public List<Map<String, Object>> getPendingForSupervisor(String supervisorUsername) {
        User supervisor = userRepo.findByUsername(supervisorUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> shiftIds = getAccessibleShiftIds(supervisor);
        if (shiftIds.isEmpty()) return List.of();

        return cashupRepo.findByStatusAndShiftIdIn("PENDING_REVIEW", shiftIds).stream()
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",          s.getId());
                    m.put("shiftId",     s.getShiftId());
                    m.put("submittedBy", s.getSubmittedBy());
                    m.put("submittedAt", s.getSubmittedAt());
                    m.put("status",      s.getStatus());
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ── Admin: paginated history of all cashup submissions ───────────────────
    //
    // Lightweight list — does NOT compute actuals (expensive). Detail view
    // uses the existing getCashupWithActuals() on demand.

    public Page<Map<String, Object>> getHistoryForAdmin(String status, String search,
                                                         LocalDateTime dateFrom, LocalDateTime dateTo,
                                                         int page, int size) {
        requireAdmin();

        Page<CashupSubmission> resultPage = cashupRepo.findHistory(
                blankToNull(status), blankToNull(search), dateFrom, dateTo,
                PageRequest.of(page, size));

        return resultPage.map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",            s.getId());
            m.put("shiftId",       s.getShiftId());
            m.put("submittedBy",   s.getSubmittedBy());
            m.put("submittedAt",   s.getSubmittedAt());
            m.put("status",        s.getStatus());
            m.put("reviewedBy",    s.getReviewedBy());
            m.put("reviewedAt",    s.getReviewedAt());
            m.put("reviewerNotes", s.getReviewerNotes());
            // Lines are EAGER-fetched — include them at no extra cost
            m.put("lines",         s.getLines() == null ? List.of() : s.getLines().stream()
                    .map(l -> {
                        Map<String, Object> line = new LinkedHashMap<>();
                        line.put("type",            l.getType());
                        line.put("currency",        l.getCurrency());
                        line.put("submittedAmount", l.getSubmittedAmount());
                        return line;
                    })
                    .collect(Collectors.toList()));
            return m;
        });
    }

    // ── Admin: full export (all matching records, no pagination) ─────────────

    public List<Map<String, Object>> getExportForAdmin(String status, String search,
                                                        LocalDateTime dateFrom, LocalDateTime dateTo) {
        requireAdmin();

        return cashupRepo.findForExport(blankToNull(status), blankToNull(search), dateFrom, dateTo)
                .stream()
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",            s.getId());
                    m.put("shiftId",       s.getShiftId());
                    m.put("submittedBy",   s.getSubmittedBy());
                    m.put("submittedAt",   s.getSubmittedAt());
                    m.put("status",        s.getStatus());
                    m.put("reviewedBy",    s.getReviewedBy());
                    m.put("reviewedAt",    s.getReviewedAt());
                    m.put("reviewerNotes", s.getReviewerNotes());

                    // Compute actuals for this shift so the export shows declared vs actual
                    List<Transaction> paidTxns = txRepo.findByStatusAndShiftId(
                            TransactionStatus.PAID, s.getShiftId());
                    Map<String, BigDecimal> actuals = computeActuals(paidTxns);

                    Set<String> allKeys = new LinkedHashSet<>();
                    if (s.getLines() != null) {
                        s.getLines().forEach(l -> allKeys.add(l.getType() + "|" + l.getCurrency()));
                    }
                    allKeys.addAll(actuals.keySet());

                    List<Map<String, Object>> comparison = new ArrayList<>();
                    for (String key : allKeys) {
                        String[] parts    = key.split("\\|", 2);
                        String type       = parts[0];
                        String currency   = parts.length > 1 ? parts[1] : "USD";
                        BigDecimal submitted = s.getLines() == null ? BigDecimal.ZERO :
                                s.getLines().stream()
                                        .filter(l -> l.getType().equals(type) && l.getCurrency().equals(currency))
                                        .map(CashupLine::getSubmittedAmount)
                                        .findFirst().orElse(BigDecimal.ZERO);
                        BigDecimal actual   = actuals.getOrDefault(key, BigDecimal.ZERO);
                        BigDecimal variance = actual.subtract(submitted);

                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("type",      type);
                        row.put("currency",  currency);
                        row.put("submitted", submitted);
                        row.put("actual",    actual);
                        row.put("variance",  variance);
                        comparison.add(row);
                    }
                    m.put("comparison", comparison);
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ── Review a cashup (supervisor/admin) ────────────────────────────────────
    //
    // correctedLines — optional updated declared amounts. When the supervisor rectifies
    //   a shortage (operator returned cash, discrepancy found), the supervisor edits the
    //   declared amounts in the UI before approving or waiving. If provided, these replace
    //   the original operator-submitted lines so the final cashup record is accurate.

    public CashupSubmission review(Long cashupId, String decision, String notes,
                                   List<CashupLineDto> correctedLines) {
        String currentUsername = getCurrentUsername();
        User currentUser = userRepo.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (currentUser.getRole() != Role.SUPERVISOR) {
            throw new AccessDeniedException("Only supervisors can review cashups");
        }

        CashupSubmission submission = cashupRepo.findById(cashupId)
                .orElseThrow(() -> new NoSuchElementException("Cashup not found: " + cashupId));

        if (!"PENDING_REVIEW".equals(submission.getStatus())) {
            throw new IllegalStateException("Cashup is not pending review (current status: " + submission.getStatus() + ")");
        }

        // Replace declared lines with supervisor-corrected amounts if provided
        if (correctedLines != null && !correctedLines.isEmpty()) {
            List<CashupLine> updated = correctedLines.stream()
                    .filter(l -> l.getSubmittedAmount() != null
                              && l.getSubmittedAmount().compareTo(BigDecimal.ZERO) >= 0)
                    .map(l -> CashupLine.builder()
                            .type(l.getType())
                            .currency(l.getCurrency())
                            .submittedAmount(l.getSubmittedAmount())
                            .build())
                    .collect(Collectors.toList());
            if (submission.getLines() == null) {
                submission.setLines(new ArrayList<>(updated));
            } else {
                submission.getLines().clear();
                submission.getLines().addAll(updated);
            }
        }

        submission.setStatus(decision);
        submission.setReviewedBy(currentUsername);
        submission.setReviewedAt(LocalDateTime.now());
        if (notes != null && !notes.isBlank()) {
            submission.setReviewerNotes(notes);
        }
        CashupSubmission reviewed = cashupRepo.save(submission);
        // Notify only supervisors at the operator's station
        String stationId = userRepo.findByUsername(reviewed.getSubmittedBy())
                .map(u -> u.getStation() != null ? u.getStation().getId() : null)
                .orElse(null);
        eventStream.broadcastCashupUpdate(stationId);
        return reviewed;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, BigDecimal> computeActuals(List<Transaction> paidTxns) {
        Map<String, BigDecimal> actuals = new LinkedHashMap<>();
        for (Transaction t : paidTxns) {
            if (t.getBreakdown() == null || t.getBreakdown().isEmpty()) continue;

            // Sum breakdown amounts in base currency to detect over-tender (cash change scenario).
            // pb.amount is the USD equivalent; pb.originalAmount is the tendered amount in original currency.
            BigDecimal breakdownSumUSD = t.getBreakdown().stream()
                    .filter(pb -> pb.getAmount() != null)
                    .map(PaymentBreakdown::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal txAmount = t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO;
            boolean overTendered = breakdownSumUSD.compareTo(txAmount) > 0
                    && breakdownSumUSD.compareTo(BigDecimal.ZERO) > 0;

            for (PaymentBreakdown pb : t.getBreakdown()) {
                if (pb.getOriginalAmount() == null) continue;

                BigDecimal net;
                if (overTendered) {
                    // Customer tendered more than the invoice (cash change scenario).
                    // Net in original currency = txAmount * originalAmount / breakdownSumUSD
                    // This derivation holds across currencies because the ratio cancels exchange rates.
                    net = txAmount.multiply(pb.getOriginalAmount())
                            .divide(breakdownSumUSD, 2, RoundingMode.HALF_UP);
                } else {
                    net = pb.getOriginalAmount();
                }

                String key = pb.getType() + "|" + pb.getOriginalCurrency();
                actuals.merge(key, net, BigDecimal::add);
            }
        }
        return actuals;
    }

    private List<String> getAccessibleShiftIds(User supervisor) {
        if (supervisor.getRole() != Role.SUPERVISOR) return List.of();
        String stationId = supervisor.getStation() != null ? supervisor.getStation().getId() : null;
        List<User> operators = stationId != null
                ? userRepo.findByStationId(stationId)
                : List.of();
        if (operators.isEmpty()) return List.of();
        List<String> usernames = operators.stream().map(User::getUsername).collect(Collectors.toList());
        return shiftRepo.findByOperatorIn(usernames).stream().map(Shift::getId).collect(Collectors.toList());
    }

    private boolean canAccessShift(String username, Shift shift) {
        if (username.equals(shift.getOperator())) return true;
        User user = userRepo.findByUsername(username).orElse(null);
        if (user == null) return false;
        if (user.getRole() == Role.SUPERVISOR) {
            User operator = userRepo.findByUsername(shift.getOperator()).orElse(null);
            if (operator == null) return false;
            return user.getStation() != null && user.getStation().equals(operator.getStation());
        }
        return false;
    }

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails ud) return ud.getUsername();
        return principal.toString();
    }

    private void requireAdmin() {
        User user = userRepo.findByUsername(getCurrentUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can access this resource");
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
