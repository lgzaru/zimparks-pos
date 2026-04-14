package com.tenten.zimparks.quotation;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/quotations")
@RequiredArgsConstructor
@Tag(name = "Quotations")
@SecurityRequirement(name = "bearerAuth")
public class QuotationController {

    private final QuotationService service;

    /**
     * List all quotations, optionally filtered.
     * GET /api/quotations
     * GET /api/quotations?status=ACTIVE
     * GET /api/quotations?customerId=C001
     */
    @GetMapping
    public List<Quotation> getAll(
            @RequestParam(required = false) QuotationStatus status,
            @RequestParam(required = false) String customerId) {
        if (status != null)     return service.findByStatus(status);
        if (customerId != null) return service.findByCustomer(customerId);
        return service.findAll();
    }

    /**
     * Search quotations by ref or customer name (case-insensitive partial match).
     * GET /api/quotations/search?q=QUO-123
     * GET /api/quotations/search?q=Acme
     */
    @GetMapping("/search")
    public List<Quotation> search(@RequestParam String q) {
        return service.search(q);
    }

    /**
     * Get a single quotation by exact ref.
     * GET /api/quotations/{ref}
     */
    @GetMapping("/{ref}")
    public Quotation getByRef(@PathVariable String ref) {
        return service.findByRef(ref);
    }

    /**
     * Create a quotation (manual entry by supervisor/admin, or ingestion from HQ/back-office).
     * Requires ADMIN role OR the CREATE_QUOTE granular permission.
     * POST /api/quotations
     */
    @PostMapping
    public Quotation create(@RequestBody Quotation quotation, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        boolean hasPermission = auth.getAuthorities().stream()
                .anyMatch(a -> "quotation:create".equals(a.getAuthority()));
        if (!isAdmin && !hasPermission) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "CREATE_QUOTE permission required to create quotations.");
        }
        return service.create(quotation);
    }

    /**
     * Manually expire a quotation so it can no longer be converted.
     * PATCH /api/quotations/{ref}/expire
     */
    @PatchMapping("/{ref}/expire")
    public Quotation expire(@PathVariable String ref) {
        return service.expire(ref);
    }
}
