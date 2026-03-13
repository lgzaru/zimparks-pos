package com.tenten.zimparks.shift;

public class NoOpenShiftException extends RuntimeException {
    public NoOpenShiftException(String message) {
        super(message);
    }
}
