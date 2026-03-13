package com.tenten.zimparks.report;

import com.tenten.zimparks.creditnote.CreditNote;
import com.tenten.zimparks.creditnote.CreditNoteRepository;
import com.tenten.zimparks.transaction.Transaction;
import com.tenten.zimparks.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository txRepo;
    private final CreditNoteRepository cnRepo;

    private static final List<String> CURRENCIES  = List.of("USD", "ZAR", "ZWG");
    private static final List<String> PAY_TYPES   = List.of(
            "Cash", "Card", "Mobile Wallet", "Coupon", "Bank Transfer", "Other");

    public List<Map<String, Object>> build(String type, Map<String, String> filters) {
        List<Transaction> paid   = txRepo.findByStatus("PAID");
        List<Transaction> all    = txRepo.findAll();
        List<CreditNote>  cns    = cnRepo.findAll();

        // Apply date/station/user/status filters
        paid = applyFilters(paid, filters);
        all  = applyFilters(all,  filters);

        return switch (type) {
            case "Shift Revenue Summary"       -> shiftRevenue(paid);
            case "Voided Transactions"         -> voided(all);
            case "Credit Notes"                -> creditNotes(cns);
            case "Currency Breakdown"          -> currencyBreakdown(paid);
            case "Payment Method Distribution" -> paymentDistribution(paid);
            case "Visitors Report"             -> visitors(paid);
            default -> List.of();
        };
    }

    // ── Filter helpers ────────────────────────────────────────────────────────
    private List<Transaction> applyFilters(List<Transaction> rows, Map<String, String> f) {
        return rows.stream().filter(t -> {
            if (f.containsKey("dateFrom") && t.getTxDate() != null
                    && t.getTxDate().compareTo(f.get("dateFrom")) < 0) return false;
            if (f.containsKey("dateTo")   && t.getTxDate() != null
                    && t.getTxDate().compareTo(f.get("dateTo"))   > 0) return false;
            if (f.containsKey("station")  && !"All".equals(f.get("station"))
                    && (t.getStation() == null || !t.getStation().getId().equals(f.get("station")))) return false;
            if (f.containsKey("user")     && !"All".equals(f.get("user"))
                    && !Objects.equals(t.getOperatorName(), f.get("user")))   return false;
            if (f.containsKey("status")   && !"All".equals(f.get("status"))
                    && !t.getStatus().equalsIgnoreCase(f.get("status")))       return false;
            return true;
        }).collect(Collectors.toList());
    }

    // ── Report builders ───────────────────────────────────────────────────────
    private List<Map<String, Object>> shiftRevenue(List<Transaction> paid) {
        return paid.stream().map(t -> Map.<String, Object>of(
                "Date",     t.getTxDate()   != null ? t.getTxDate()   : "",
                "Time",     t.getTxTime()   != null ? t.getTxTime()   : "",
                "Operator", t.getOperatorName() != null ? t.getOperatorName() : "—",
                "Station",  t.getStation()  != null ? t.getStation().getId() : "—",
                "Ref",      t.getRef(),
                "Amount",   fmt(t.getAmount(), t.getCurrency())
        )).collect(Collectors.toList());
    }

    private List<Map<String, Object>> voided(List<Transaction> all) {
        return all.stream()
                .filter(t -> "VOIDED".equals(t.getStatus()))
                .map(t -> Map.<String, Object>of(
                        "Date",      t.getTxDate()      != null ? t.getTxDate()      : "",
                        "Reference", t.getRef(),
                        "Customer",  t.getCustomerName() != null ? t.getCustomerName() : "",
                        "Station",   t.getStation()     != null ? t.getStation().getId() : "—",
                        "Amount",    fmt(t.getAmount(), t.getCurrency()),
                        "Voided By", t.getVoidedBy()    != null ? t.getVoidedBy()    : "—",
                        "Reason",    t.getVoidReason()  != null ? t.getVoidReason()  : "—"
                )).collect(Collectors.toList());
    }

    private List<Map<String, Object>> creditNotes(List<CreditNote> cns) {
        return cns.stream().map(c -> Map.<String, Object>of(
                "Date",        c.getNoteDate() != null ? c.getNoteDate() : "",
                "ID",          c.getId(),
                "Booking Ref", c.getTxRef()   != null ? c.getTxRef()   : "",
                "Client",      c.getClient()  != null ? c.getClient()  : "",
                "Reason",      c.getReason()  != null ? c.getReason().getDescription() : "",
                "Amount",      fmt(c.getAmount(), "USD"),
                "Status",      c.getStatus()
        )).collect(Collectors.toList());
    }

    private List<Map<String, Object>> currencyBreakdown(List<Transaction> paid) {
        return CURRENCIES.stream().map(cur -> {
            BigDecimal total = paid.stream()
                    .filter(t -> cur.equals(t.getCurrency()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long count = paid.stream().filter(t -> cur.equals(t.getCurrency())).count();
            return Map.<String, Object>of(
                    "Currency",     cur,
                    "Total Amount", fmt(total, cur),
                    "Transactions", count
            );
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> paymentDistribution(List<Transaction> paid) {
        BigDecimal totalRev = paid.stream()
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return PAY_TYPES.stream().map(pt -> {
                    BigDecimal amt = paid.stream()
                            .flatMap(t -> t.getBreakdown() != null ? t.getBreakdown().stream() : java.util.stream.Stream.empty())
                            .filter(b -> pt.equals(b.getType()))
                            .map(b -> b.getAmount())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    String share = totalRev.compareTo(BigDecimal.ZERO) > 0
                            ? amt.multiply(BigDecimal.valueOf(100))
                            .divide(totalRev, 1, RoundingMode.HALF_UP) + "%"
                            : "0%";

                    return Map.<String, Object>of(
                            "Payment Method", pt,
                            "Total Amount",   fmt(amt, "USD"),
                            "Share",          share
                    );
                }).filter(r -> !r.get("Total Amount").equals("USD 0.00"))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> visitors(List<Transaction> paid) {
        return paid.stream()
                .filter(t -> List.of("ENT001","ENT002").contains(t.getProductCode()))
                .map(t -> Map.<String, Object>of(
                        "Date",      t.getTxDate()       != null ? t.getTxDate()       : "",
                        "Reference", t.getRef(),
                        "Customer",  t.getCustomerName() != null ? t.getCustomerName() : "",
                        "Product",   t.getProductCode()  != null ? t.getProductCode()  : "",
                        "Visitors",  t.getItems(),
                        "Station",   t.getStation()      != null ? t.getStation().getId() : "—",
                        "Operator",  t.getOperatorName() != null ? t.getOperatorName() : "—"
                )).collect(Collectors.toList());
    }

    private String fmt(BigDecimal n, String currency) {
        return (currency != null ? currency : "USD") + " " +
                (n != null ? n.setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00");
    }
}
