package com.tenten.zimparks.creditnote;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Locale;

@Getter
public enum CreditNoteReason {
    PERSONAL_ILLNESS("Personal Illness"),
    NATIONAL_DISASTER("National Disaster");

    private final String description;

    CreditNoteReason(String description) {
        this.description = description;
    }

    @JsonCreator
    public static CreditNoteReason fromValue(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim().toUpperCase(Locale.ROOT).replace(" ", "_");
        try {
            return CreditNoteReason.valueOf(v);
        } catch (IllegalArgumentException e) {
            for (CreditNoteReason reason : values()) {
                if (reason.description.equalsIgnoreCase(value)) {
                    return reason;
                }
            }
            throw e;
        }
    }

    @JsonValue
    public String toValue() {
        return description;
    }
}
