package com.tenten.zimparks.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for online (mobile money) payments.
 *
 * Flow:
 *  1. Frontend → POST /api/online-payments/initiate  (JWT required)
 *     Returns paynowRef + PENDING status; USSD push sent to customer's phone.
 *  2. Paynow  → POST /api/online-payments/callback/{ref}  (PUBLIC — no JWT)
 *     Paynow calls this when payment status changes; triggers transaction creation on PAID.
 *  3. Frontend → GET  /api/online-payments/status/{ref}  (JWT required)
 *     Frontend polls this every ~5 s; also drives Paynow polling as a fallback.
 *     Returns txRef once PAID so frontend can display the receipt.
 */
@RestController
@RequestMapping("/api/online-payments")
@RequiredArgsConstructor
public class OnlinePaymentController {

    private final OnlinePaymentService service;

    @PostMapping("/initiate")
    public ResponseEntity<OnlinePaymentResponse> initiate(@RequestBody OnlinePaymentRequest request) {
        return ResponseEntity.ok(service.initiate(request));
    }

    @PostMapping("/callback/{ref}")
    public ResponseEntity<Void> callback(@PathVariable String ref) {
        service.handleCallback(ref);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status/{ref}")
    public ResponseEntity<OnlinePaymentResponse> status(@PathVariable String ref) {
        return ResponseEntity.ok(service.getStatus(ref));
    }
}
