package com.zidio.keystone.storage;

import java.util.Optional;
import java.util.UUID;

/**
 * Where work order photo bytes live. Two implementations, picked by
 * keystone.storage.type: "db" (default — bytes stay inline in Postgres) and
 * "s3" (any S3-compatible cloud object store).
 */
public interface AttachmentStorage {

    /**
     * Store the bytes. Returns the object key if they were placed in external
     * storage, or empty if the caller should keep them inline in the database.
     */
    Optional<String> store(UUID workOrderId, byte[] bytes, String contentType);

    /** Read back the bytes for a key previously returned by {@link #store}. */
    byte[] fetch(String key);
}
