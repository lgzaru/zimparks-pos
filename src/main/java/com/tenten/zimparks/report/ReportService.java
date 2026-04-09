package com.tenten.zimparks.report;

import com.tenten.zimparks.bank.BankRepository;
import com.tenten.zimparks.creditnote.CreditNote;
import com.tenten.zimparks.creditnote.CreditNoteRepository;
import com.tenten.zimparks.station.StationRepository;
import com.tenten.zimparks.transaction.Transaction;
import com.tenten.zimparks.transaction.TransactionRepository;
import com.tenten.zimparks.transaction.TransactionStatus;
import com.tenten.zimparks.vat.VatRepository;
import com.tenten.zimparks.vat.VatSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository txRepo;
    private final CreditNoteRepository cnRepo;
    private final BankRepository bankRepo;
    private final VatRepository vatRepo;
    private final StationRepository stationRepo;

    private static final List<String> CURRENCIES  = List.of("USD", "ZAR", "ZWG");
    private static final DateTimeFormatter TX_DF   = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final List<String> PAY_TYPES   = List.of(
            "Cash", "Card", "Mobile Wallet", "Coupon", "Bank Transfer", "Other");

    public Page<Map<String, Object>> build(String type, Map<String, String> filters, Pageable pageable) {
        List<Transaction> paid   = txRepo.findByStatus(TransactionStatus.PAID);
        List<Transaction> all    = txRepo.findAll();
        List<CreditNote>  cns    = cnRepo.findAll();

        // Apply date/station/user/status filters
        paid = applyFilters(paid, filters);
        all  = applyFilters(all,  filters);

        List<Map<String, Object>> result = switch (type) {
            case "Shift Revenue Summary"       -> shiftRevenue(paid);
            case "Voided Transactions"         -> voided(all);
            case "Credit Notes"                -> creditNotes(cns);
            case "Currency Breakdown"          -> currencyBreakdown(paid);
            case "Payment Method Distribution" -> paymentDistribution(paid);
            case "Visitors Report"             -> visitors(paid);
            case "Vision Report"               -> visionReport(sortedByDate(paid, filters));
            default -> List.of();
        };

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), result.size());

        if (start > result.size()) {
            return new PageImpl<>(List.of(), pageable, result.size());
        }

        return new PageImpl<>(result.subList(start, end), pageable, result.size());
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
                    && !t.getStatus().name().equalsIgnoreCase(f.get("status")))       return false;
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
                .filter(t -> TransactionStatus.VOIDED.equals(t.getStatus()))
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

    // ── Sort transactions by txDate ("dd MMM yyyy") using explicit sortDir filter ──
    private List<Transaction> sortedByDate(List<Transaction> txs, Map<String, String> filters) {
        boolean desc = !"asc".equalsIgnoreCase(filters.get("sortDir")); // default desc
        Comparator<Transaction> cmp = Comparator.comparing(t -> {
            try { return LocalDate.parse(t.getTxDate(), TX_DF); }
            catch (Exception e) { return LocalDate.MIN; }
        });
        if (desc) cmp = cmp.reversed();
        return txs.stream().sorted(cmp).collect(Collectors.toList());
    }

    // ── Vision Report ─────────────────────────────────────────────────────────
    private List<Map<String, Object>> visionReport(List<Transaction> paid) {
        VatSettings vat = vatRepo.findById(1L).orElseGet(() -> {
            VatSettings v = new VatSettings();
            v.setId(1L);
            v.setZwgRate(BigDecimal.valueOf(15.0));
            v.setOtherRate(BigDecimal.valueOf(15.5));
            return v;
        });

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Transaction t : paid) {
            String txDate   = t.getTxDate()       != null ? t.getTxDate()       : "";
            String ref      = t.getRef()           != null ? t.getRef()           : "";
            String descr    = visionDescr(t);
            String ccy      = visionCcy(t);
            String cluster  = t.getStation()      != null ? t.getStation().getId() : "";
            String product  = t.getProductCode()  != null ? t.getProductCode()  : "";

            BigDecimal total   = t.getAmount()    != null ? t.getAmount()    : BigDecimal.ZERO;
            BigDecimal vatAmt  = t.getVatAmount() != null ? t.getVatAmount() : BigDecimal.ZERO;
            BigDecimal exclVat = total.subtract(vatAmt);

            String revAcct  = vat.getRevenueAccount() != null ? vat.getRevenueAccount() : "";
            String vatAcct  = vat.getVatAccount()     != null ? vat.getVatAccount()     : "";
            String bankAcct = visionBankAcct(t);

            // Line 1 — revenue excl. VAT, Credit
            rows.add(visionRow(txDate, revAcct, ref, descr, ccy, fmtNum(exclVat), "C", cluster, product, ""));
            // Line 2 — VAT amount, Credit
            rows.add(visionRow(txDate, vatAcct, ref, descr, ccy, fmtNum(vatAmt),  "C", cluster, product, "S"));
            // Line 3 — total settlement, Debit
            rows.add(visionRow(txDate, bankAcct, ref, descr, ccy, fmtNum(total),  "D", cluster, product, ""));
        }
        return rows;
    }

    private Map<String, Object> visionRow(String txDate, String account, String ref, String descr,
                                          String ccy, String amount, String dc,
                                          String cluster, String product, String vat) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("TransactionDate", txDate);
        row.put("Account",         account);
        row.put("Reference",       ref);
        row.put("Description",     descr);
        row.put("PaymentCurrency", ccy);
        row.put("Amount",          amount);
        row.put("DebitCredit",     dc);
        row.put("Cluster",         cluster);
        row.put("Product",         product);
        row.put("VAT",             vat);
        return row;
    }

    private String visionDescr(Transaction t) {
        if (t.getItemsList() != null && !t.getItemsList().isEmpty()
                && t.getItemsList().get(0).getDescr() != null) {
            return t.getItemsList().get(0).getDescr();
        }
        return t.getProductCode() != null ? t.getProductCode() : "";
    }

    private String visionCcy(Transaction t) {
        if (t.getReceipt() != null && t.getReceipt().getOriginalCurrency() != null) {
            return t.getReceipt().getOriginalCurrency();
        }
        return t.getCurrency() != null ? t.getCurrency() : "USD";
    }

    private String visionBankAcct(Transaction t) {
        // Prefer the bank explicitly recorded on the transaction
        if (t.getBankCode() != null) {
            return bankRepo.findById(t.getBankCode())
                    .map(b -> b.getAccountNumber() != null ? b.getAccountNumber() : "")
                    .orElse("");
        }
        // Fall back to the station's first linked bank (single-bank stations never set bankCode)
        if (t.getStation() != null) {
            return stationRepo.findById(t.getStation().getId())
                    .flatMap(s -> s.getBanks().stream()
                            .filter(b -> b.getAccountNumber() != null && !b.getAccountNumber().isBlank())
                            .findFirst())
                    .map(b -> b.getAccountNumber())
                    .orElse("");
        }
        return "";
    }

    private String fmtNum(BigDecimal n) {
        return n != null ? n.setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00";
    }
}
