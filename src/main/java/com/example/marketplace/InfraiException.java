package com.example.marketplace;

public final class InfraiException extends RuntimeException {
    private final int status;
    public InfraiException(String code, int status) {
        super(code);
        this.status = status;
    }
    public int status() { return status; }
}
