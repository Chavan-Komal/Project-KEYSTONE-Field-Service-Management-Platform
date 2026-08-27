package com.zidio.keystone.exception;

/** Thrown when logging parts usage would take stock below zero. Maps to HTTP 409. */
public class InsufficientStockException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InsufficientStockException(String message) {
        super(message);
    }
}
