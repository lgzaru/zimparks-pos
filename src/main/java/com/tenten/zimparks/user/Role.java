package com.tenten.zimparks.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;
import java.util.Set;

import static com.tenten.zimparks.user.Permission.*;

@RequiredArgsConstructor
public enum Role {
    OPERATOR(Set.of(
            CLOSE_SHIFT
    )),
    SUPERVISOR(Set.of(
            CLOSE_SHIFT,
            UPDATE_PRODUCT_PRICING,
            ADD_PRODUCT,
            LINK_BANKS,
            UNLINK_BANKS
    )),
    ADMIN(Set.of(
            CLOSE_SHIFT,
            UPDATE_PRODUCT_PRICING,
            ADD_PRODUCT,
            LINK_BANKS,
            UNLINK_BANKS
    ));

    @Getter
    private final Set<Permission> permissions;

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
