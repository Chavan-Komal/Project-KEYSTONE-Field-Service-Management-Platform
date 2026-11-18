package com.zidio.keystone.storage;

/** Cloud storage was unreachable or rejected the request. Mapped to HTTP 502. */
public class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
