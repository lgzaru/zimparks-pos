package com.tenten.zimparks.fiscalization;

/**
 * Mirrors ZIMRA spec §8.2.1 validation error severity colours.
 *
 * GREY   – chain-integrity violation (previous receipt missing); revalidated by FDMS
 *          when the missing receipt eventually arrives. Fiscal day cannot auto-close
 *          while any GREY receipt is present.
 * YELLOW – minor violation; fiscal day may still auto-close.
 * RED    – major violation; fiscal day cannot auto-close.
 */
public enum ValidationSeverity {
    GREY,
    YELLOW,
    RED
}
