package com.tenten.zimparks.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum Role {
    OPERATOR,
    SUPERVISOR,
    ADMIN;

    @JsonCreator
    public static Role fromValue(String value) {
        if (value == null) {
            return null;
        }
        return Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
