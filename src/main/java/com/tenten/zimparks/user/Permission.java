package com.tenten.zimparks.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Permission {
    LINK_BANKS("station:link-banks"),
    UNLINK_BANKS("station:unlink-banks"),
    UPDATE_PRODUCT_PRICING("product:update-pricing"),
    ADD_PRODUCT("product:add-product"),
    CLOSE_SHIFT("shift:close");

    @Getter
    private final String permission;
}
