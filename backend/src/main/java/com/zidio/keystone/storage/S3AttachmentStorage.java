package com.zidio.keystone.storage;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Optional;
import java.util.UUID;

/**
 * Cloud object storage over the S3 API — works with AWS S3 and S3-compatible
 * stores (Cloudflare R2, MinIO, Supabase Storage, ...). Object keys are
 * "work-orders/{workOrderId}/{random uuid}"; the original filename lives in
 * the database row, never in the key, so nothing user-supplied reaches storage.
 */
public class S3AttachmentStorage implements AttachmentStorage {

    private final S3Client s3;
    private final String bucket;

    public S3AttachmentStorage(S3Client s3, String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    @Override
    public Optional<String> store(UUID workOrderId, byte[] bytes, String contentType) {
        String key = "work-orders/" + workOrderId + "/" + UUID.randomUUID();
        try {
            s3.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(bytes));
        } catch (RuntimeException ex) {
            throw new StorageException("Could not upload the photo to cloud storage.", ex);
        }
        return Optional.of(key);
    }

    @Override
    public byte[] fetch(String key) {
        try {
            return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
        } catch (RuntimeException ex) {
            throw new StorageException("Could not read the photo from cloud storage.", ex);
        }
    }
}
