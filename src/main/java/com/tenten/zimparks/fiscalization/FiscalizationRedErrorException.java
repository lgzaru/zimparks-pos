package com.tenten.zimparks.fiscalization;

import lombok.Getter;

@Getter
public class FiscalizationRedErrorException extends RuntimeException {
    private final String summary;

    public FiscalizationRedErrorException(String summary) {
        super(summary);
        this.summary = summary;
    }
}
