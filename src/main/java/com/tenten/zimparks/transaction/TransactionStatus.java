package com.tenten.zimparks.transaction;

public enum TransactionStatus {
    PAID,
    VOIDED,
    VOID_PENDING,    // replaces "PENDING_VOID"
    CN_PENDING,      // replaces "PendingCN"
    VOID_REJECTED
}
