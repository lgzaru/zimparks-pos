package com.tenten.zimparks.fiscalization;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FiscalizationValidationError {
    private String code;
    private String message;
    private ValidationSeverity severity;
}
