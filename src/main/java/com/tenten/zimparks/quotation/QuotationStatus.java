package com.tenten.zimparks.quotation;

public enum QuotationStatus {
    ACTIVE,    // Ready to be loaded and converted to a transaction
    EXPIRED,   // Past expiry date or manually expired — cannot be converted
    CONVERTED  // Already converted to a transaction
}
