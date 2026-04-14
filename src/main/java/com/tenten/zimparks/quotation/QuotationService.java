package com.tenten.zimparks.quotation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuotationService {

    private final QuotationRepository repo;

    public List<Quotation> findAll() {
        return repo.findAll();
    }

    public List<Quotation> findByStatus(QuotationStatus status) {
        return repo.findByStatus(status);
    }

    public List<Quotation> findByCustomer(String customerId) {
        return repo.findByCustomerId(customerId);
    }

    /** Case-insensitive search by quotation ref or customer name. */
    public List<Quotation> search(String q) {
        return repo.findByRefContainingIgnoreCaseOrCustomerNameContainingIgnoreCase(q, q);
    }

    public Quotation findByRef(String ref) {
        return repo.findById(ref)
                .orElseThrow(() -> new RuntimeException("Quotation not found: " + ref));
    }

    public Quotation create(Quotation quotation) {
        if (quotation.getStatus() == null) {
            quotation.setStatus(QuotationStatus.ACTIVE);
        }
        if (quotation.getItems() != null) {
            quotation.getItems().forEach(item -> item.setQuotationRef(quotation.getRef()));
        }
        return repo.save(quotation);
    }

    /** Manually expires a quotation. Throws if it has already been converted. */
    public Quotation expire(String ref) {
        Quotation q = findByRef(ref);
        if (q.getStatus() == QuotationStatus.CONVERTED) {
            throw new RuntimeException(
                    "Quotation " + ref + " has already been converted to transaction " + q.getConvertedTxnRef());
        }
        q.setStatus(QuotationStatus.EXPIRED);
        return repo.save(q);
    }

    /**
     * Validates that a quotation can be converted to a transaction.
     * Auto-expires quotations past their expiry date.
     *
     * @throws RuntimeException if expired or already converted
     */
    public Quotation validateForConversion(String ref) {
        Quotation q = findByRef(ref);

        if (q.getStatus() == QuotationStatus.CONVERTED) {
            throw new RuntimeException(
                    "Quotation " + ref + " has already been converted to transaction " + q.getConvertedTxnRef());
        }

        // Auto-expire if the expiry date has passed (even if status is still ACTIVE)
        if (q.getExpiryDate() != null && q.getExpiryDate().isBefore(LocalDate.now())) {
            q.setStatus(QuotationStatus.EXPIRED);
            repo.save(q);
            throw new RuntimeException(
                    "Quotation " + ref + " expired on " + q.getExpiryDate() + " and cannot be converted.");
        }

        if (q.getStatus() == QuotationStatus.EXPIRED) {
            throw new RuntimeException("Quotation " + ref + " has expired and cannot be converted.");
        }

        return q;
    }

    /**
     * Marks a quotation as CONVERTED once the corresponding transaction has been saved.
     * Called by TransactionService after a successful transaction creation.
     */
    public void markConverted(String ref, String txnRef) {
        Quotation q = findByRef(ref);
        q.setStatus(QuotationStatus.CONVERTED);
        q.setConvertedTxnRef(txnRef);
        repo.save(q);
    }
}
