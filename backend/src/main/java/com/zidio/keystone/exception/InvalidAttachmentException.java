package com.zidio.keystone.exception;

/** Thrown when an uploaded attachment is empty, not an image, or too large. Maps to HTTP 400. */
public class InvalidAttachmentException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidAttachmentException(String message) {
        super(message);
    }
}
