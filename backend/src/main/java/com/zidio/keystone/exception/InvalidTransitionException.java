package com.zidio.keystone.exception;

/** Thrown when a work-order status change would violate the guarded lifecycle. Maps to HTTP 409. */
public class InvalidTransitionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidTransitionException(String message) {
        super(message);
    }
}
