package com.zidio.keystone.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Default: nothing external — photo bytes stay inline in Postgres. */
@Component
@ConditionalOnProperty(name = "keystone.storage.type", havingValue = "db", matchIfMissing = true)
public class DatabaseAttachmentStorage implements AttachmentStorage {

    @Override
    public Optional<String> store(UUID workOrderId, byte[] bytes, String contentType) {
        return Optional.empty();
    }

    @Override
    public byte[] fetch(String key) {
        throw new IllegalStateException(
            "This photo is stored in cloud storage (key " + key + ") but keystone.storage.type is 'db'. "
                + "Set STORAGE_TYPE=s3 and the S3_* settings to read it.");
    }
}
