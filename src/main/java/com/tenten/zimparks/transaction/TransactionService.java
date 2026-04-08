package com.tenten.zimparks.transaction;


import com.tenten.zimparks.event.EventStreamController;
import com.tenten.zimparks.currency.Currency;
import com.tenten.zimparks.currency.CurrencyService;
import com.tenten.zimparks.fiscalization.FiscalizationBridgeService;
import com.tenten.zimparks.shift.NoOpenShiftException;
import com.tenten.zimparks.shift.ShiftRepository;
import com.tenten.zimparks.station.StationRepository;
import com.tenten.zimparks.user.Role;
import com.tenten.zimparks.user.User;
import com.tenten.zimparks.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository repo;
    private final StationRepository stationRepo;
    private final CurrencyService currencyService;
    private final ReceiptRepository receiptRepo;
    private final UserRepository userRepo;
    private final ShiftRepository shiftRepo;
    private final com.tenten.zimparks.vat.VatService vatService;
    private final EventStreamController eventStream;
    private final com.tenten.zimparks.product.ProductRepository productRepo;
    private final EntryTransactionRepository entryTxRepo;

    private final FiscalizationBridgeService fiscalizationBridgeService;

    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + username));
    }

    public List<Transaction> findAll() {
        User user = getCurrentUser();
        if (user.getRole() == Role.ADMIN) {
            return repo.findAll();
        }
        return repo.findByStationId(user.getStation().getId());
    }

    public List<Transaction> findByShiftId(String shiftId) {
        return repo.findByShiftId(shiftId);
    }

    public List<Transaction> findByStatus(TransactionStatus status) {
        User user = getCurrentUser();
        if (user.getRole() == Role.ADMIN) {
            return repo.findByStatus(status);
        }
        return repo.findByStatusAndStationId(status, user.getStation().getId());
    }

    public List<Transaction> findByCustomer(String custId) {
        User user = getCurrentUser();
        if (user.getRole() == Role.ADMIN) {
            return repo.findByCustomerId(custId);
        }
        return repo.findByCustomerIdAndStationId(custId, user.getStation().getId());
    }

    public Transaction findByRef(String ref) {
        return repo.findById(ref)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + ref));
    }

    public Receipt findReceiptByRef(String ref) {
        return receiptRepo.findById(ref)
                .orElseThrow(() -> new RuntimeException("Receipt not found for transaction: " + ref));
    }

    public Transaction create(Transaction tx) {
        User user = getCurrentUser();
        var shift = shiftRepo.findTopByOperatorOrderByStartFullDesc(user.getUsername())
                .filter(s -> "Open".equals(s.getStatus()))
                .orElseThrow(() -> new NoOpenShiftException("no open shifts are available"));

        String ref = "TXN-" + String.valueOf(System.currentTimeMillis()).substring(6);
        LocalDateTime now = LocalDateTime.now();
        tx.setRef(ref);
        tx.setStatus(TransactionStatus.PAID);
        tx.setOperatorUsername(user.getUsername());
        tx.setTxTime(now.format(TF));
        tx.setTxDate(now.format(DF));
        // If shiftId is not provided in request, use the current active shift
        if (tx.getShiftId() == null) {
            tx.setShiftId(shift.getId());
        }

        // Link to bank
        var station = user.getStation();
        if (station != null && station.getBanks() != null) {
            var banks = station.getBanks();
            if (banks.isEmpty()) {
                throw new RuntimeException(String.format("No banks linked yet to the %s contact System Admin or a Supervisor to resolve this", station.getName()));
            } else if (banks.size() == 1) {
                // Only one bank linked to station, select it automatically
                tx.setBankCode(banks.get(0).getCode());
            } else if (banks.size() > 1) {
                // Multiple banks linked, person should choose one
                if (tx.getBankCode() == null || tx.getBankCode().isBlank()) {
                    throw new RuntimeException("Multiple banks linked to station, please select a bank");
                }
                // Verify that the chosen bank is linked to the station
                boolean bankIsLinked = banks.stream()
                        .anyMatch(b -> b.getCode().equals(tx.getBankCode()));
                if (!bankIsLinked) {
                    throw new RuntimeException("Selected bank is not linked to this station");
                }
            }
        } 

        // payload.amount from the frontend is always the cart total in USD base currency.
        BigDecimal baseAmount = tx.getAmount();
        String baseCurrency = "USD";

        // Determine payment currency — all breakdown entries share the same currency (no mixed currencies per receipt).
        String currencyFromTx = tx.getCurrency();
        if (tx.getBreakdown() != null && !tx.getBreakdown().isEmpty()) {
            currencyFromTx = tx.getBreakdown().get(0).getCurrency();
        }
        final String originalCurrency = currencyFromTx != null ? currencyFromTx : "USD";

        // VAT rate is selected by payment currency (ZWG rate vs other rate).
        // VAT is calculated on the base USD amount and stored in USD on the transaction.
        var vatSettings = vatService.get();
        BigDecimal vatRate = "ZWG".equalsIgnoreCase(originalCurrency) ? vatSettings.getZwgRate() : vatSettings.getOtherRate();
        // Tax-inclusive: vatAmount = amount * rate / (100 + rate)
        BigDecimal vatDivisor = vatRate.add(new BigDecimal("100")).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal amountExclVat = baseAmount.divide(vatDivisor, 2, RoundingMode.HALF_UP);
        BigDecimal baseVatAmount = baseAmount.subtract(amountExclVat); // in USD

        tx.setVatRate(vatRate);

        // Transaction record is always stored in base currency (USD).
        tx.setAmount(baseAmount);
        tx.setVatAmount(baseVatAmount);
        tx.setCurrency(baseCurrency);

        // Convert USD amounts to the original payment currency for the receipt
        // (used by fiscalization and receipt display).
        BigDecimal originalCurrencyAmount = baseAmount;
        BigDecimal originalCurrencyVatAmount = baseVatAmount;
        if (!baseCurrency.equalsIgnoreCase(originalCurrency)) {
            Currency cur = currencyService.findById(originalCurrency)
                    .orElseThrow(() -> new RuntimeException("Currency not found: " + originalCurrency));
            // USD → original currency: multiply by exchange rate
            originalCurrencyAmount = baseAmount.multiply(cur.getExchangeRate()).setScale(2, RoundingMode.HALF_UP);
            originalCurrencyVatAmount = baseVatAmount.multiply(cur.getExchangeRate()).setScale(2, RoundingMode.HALF_UP);
        }

        Receipt receipt = Receipt.builder()
                .txRef(ref)
                .originalCurrency(originalCurrency)
                .originalAmount(originalCurrencyAmount)   // in originalCurrency (e.g. ZWG)
                .baseCurrency(baseCurrency)
                .baseAmount(baseAmount)                   // always USD
                .receiptNumber("REC-" + ref.substring(4))
                .status(TransactionStatus.PAID)
                .vatRate(vatRate)
                .vatAmount(originalCurrencyVatAmount)     // in originalCurrency for fiscal display
                .shiftId(tx.getShiftId())
                .build();
        tx.setReceipt(receipt);

        if (tx.getBreakdown() != null) {
            tx.getBreakdown().forEach(b -> {
                b.setTxRef(ref);
                b.setOriginalAmount(b.getAmount());
                b.setOriginalCurrency(b.getCurrency());

                // Also convert breakdown amounts to base currency if they are in different currency
                if (b.getCurrency() != null && !baseCurrency.equals(b.getCurrency())) {
                    Currency cur = currencyService.findById(b.getCurrency())
                            .orElseThrow(() -> new RuntimeException("Currency not found: " + b.getCurrency()));
                    b.setAmount(b.getAmount().divide(cur.getExchangeRate(), 2, RoundingMode.HALF_UP));
                    b.setCurrency(baseCurrency);
                }
            });
        }
        if (tx.getVehicle() != null)
            tx.getVehicle().setTxRef(ref);

        // Process Entry Transactions
        if (tx.getItemsList() != null && !tx.getItemsList().isEmpty()) {
            for (TransactionItem item : tx.getItemsList()) {
                item.setTxRef(ref);
                productRepo.findById(new com.tenten.zimparks.product.ProductId(item.getProductCode(), user.getStation().getId()))
                        .ifPresent(p -> {
                            // Enrich item with HS code from product record (server is authoritative)
                            if (p.getHsCode() != null && !p.getHsCode().isBlank()) {
                                item.setHsCode(p.getHsCode());
                            }
                            if (Boolean.TRUE.equals(p.getEntryProduct())) {
                                for (int i = 0; i < item.getQuantity(); i++) {
                                    EntryTransaction entry = EntryTransaction.builder()
                                            .txRef(ref)
                                            .productCode(p.getId().getCode())
                                            .productDescr(p.getDescr())
                                            .amount(item.getUnitPrice()) // Use unit price for each entry
                                            .createdAt(now)
                                            .build();
                                    entryTxRepo.save(entry);
                                }
                                tx.setHasEntry(true);
                            }
                        });
            }
        } else if (tx.getProductCode() != null) {
            // Backward compatibility for single product transaction
            productRepo.findById(new com.tenten.zimparks.product.ProductId(tx.getProductCode(), user.getStation().getId()))
                    .ifPresent(p -> {
                        if (Boolean.TRUE.equals(p.getEntryProduct())) {
                            int count = tx.getItems() != null ? tx.getItems() : 1;
                            for (int i = 0; i < count; i++) {
                                EntryTransaction entry = EntryTransaction.builder()
                                        .txRef(ref)
                                        .productCode(p.getId().getCode())
                                        .productDescr(p.getDescr())
                                        .amount(p.getPrice())
                                        .createdAt(now)
                                        .build();
                                entryTxRepo.save(entry);
                            }
                            tx.setHasEntry(true);
                        }
                    });
        }

        Transaction saved = repo.save(tx);
        eventStream.broadcastTxUpdate();

        // Fiscalize — updates receipt with fiscal data if successful
        if(saved.getVirtualDeviceId() != null) {
            fiscalizationBridgeService.fiscalize(saved);
        }

        // Reload from DB to include fiscal data populated by fiscalize()
        return repo.findById(saved.getRef()).orElse(saved);
    }

        public Transaction voidTx(String ref, VoidTransactionRequest body) {
            User user = getCurrentUser();
            Transaction tx = repo.findById(ref)
                    .orElseThrow(() -> new RuntimeException("Transaction not found: " + ref));

            if (TransactionStatus.VOIDED.equals(tx.getStatus()))
                throw new IllegalStateException("Already voided");

            // All roles (Operator, Supervisor, Admin) only initiate a void request
            tx.setStatus(TransactionStatus.VOID_PENDING);
            tx.setVoidReason(body.getReason());
            tx.setVoidRequestedBy(user.getUsername());

            Transaction saved = repo.save(tx);
            eventStream.broadcastTxUpdate();
            return saved;
        }

        public Transaction approveVoid(String ref) {
            User user = getCurrentUser();
            Transaction tx = repo.findById(ref)
                    .orElseThrow(() -> new RuntimeException("Transaction not found: " + ref));

            if (!TransactionStatus.VOID_PENDING.equals(tx.getStatus())) {
                throw new IllegalStateException("Transaction is not pending void");
            }

            tx.setStatus(TransactionStatus.VOIDED);
            tx.setVoidedBy(user.getUsername());
            if (tx.getReceipt() != null) {
                tx.getReceipt().setStatus(TransactionStatus.VOIDED);
            }
            Transaction saved = repo.save(tx);
            eventStream.broadcastTxUpdate();
            return saved;
        }

        public Transaction rejectVoid(String ref, String reason) {
            User user = getCurrentUser();
            Transaction tx = repo.findById(ref)
                    .orElseThrow(() -> new RuntimeException("Transaction not found: " + ref));

            if (!TransactionStatus.VOID_PENDING.equals(tx.getStatus())) {
                throw new IllegalStateException("Transaction is not pending void");
            }

            tx.setStatus(TransactionStatus.VOID_REJECTED);
            if (reason != null) {
                tx.setVoidReason(tx.getVoidReason() + " [REJECTED: " + reason + "]");
            }
            Transaction saved = repo.save(tx);
            eventStream.broadcastTxUpdate();
            return saved;
        }

        public Transaction update(String ref, Transaction patch) {
            Transaction tx = repo.findById(ref)
                    .orElseThrow(() -> new RuntimeException("Transaction not found: " + ref));

            if (patch.getProductCode() != null) tx.setProductCode(patch.getProductCode());
            if (patch.getItems() != null) tx.setItems(patch.getItems());
            if (patch.getAmount() != null) tx.setAmount(patch.getAmount());
            if (patch.getStatus() != null) tx.setStatus(patch.getStatus());
            if (patch.getCurrency() != null) tx.setCurrency(patch.getCurrency());
            if (patch.getCustomerName() != null) tx.setCustomerName(patch.getCustomerName());
            if (patch.getCustomerId() != null) tx.setCustomerId(patch.getCustomerId());
            if (patch.getOperatorName() != null) tx.setOperatorName(patch.getOperatorName());
            if (patch.getStation() != null) tx.setStation(patch.getStation());
            if (patch.getBankCode() != null) tx.setBankCode(patch.getBankCode());
            if (patch.getVatRate() != null) tx.setVatRate(patch.getVatRate());
            if (patch.getVatAmount() != null) tx.setVatAmount(patch.getVatAmount());
            if (patch.getShiftId() != null) tx.setShiftId(patch.getShiftId());
            if (patch.getVirtualDeviceId() != null) tx.setVirtualDeviceId(patch.getVirtualDeviceId());
            if (patch.getHasEntry() != null) tx.setHasEntry(patch.getHasEntry());

            Transaction saved = repo.save(tx);
            eventStream.broadcastTxUpdate();
            return saved;
        }

        public void delete(String ref) {
            if (!repo.existsById(ref)) {
                throw new RuntimeException("Transaction not found: " + ref);
            }
            repo.deleteById(ref);
            eventStream.broadcastTxUpdate();
        }

        // --- Entry Transaction Methods ---

        public List<EntryTransaction> findAllEntryTransactions() {
            return entryTxRepo.findAll();
        }

        public List<EntryTransaction> findEntriesByTxRef(String txRef) {
            return entryTxRepo.findByTxRef(txRef);
        }

        public EntryTransaction findEntryById(Long id) {
            return entryTxRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Entry transaction not found: " + id));
        }
    }
