package com.zidio.keystone;

import com.zidio.keystone.storage.S3AttachmentStorage;
import com.zidio.keystone.storage.StorageException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks what S3AttachmentStorage actually asks S3 to do. Uses a JDK proxy
 * over the S3Client interface rather than Mockito, whose bundled bytecode
 * library can't yet mock under Java 26.
 */
class S3AttachmentStorageTest {

    private final List<Object> calls = new ArrayList<>();
    private byte[] storedBytes = null;
    private boolean failPuts = false;

    private final S3Client s3 = (S3Client) Proxy.newProxyInstance(
        S3Client.class.getClassLoader(), new Class<?>[]{S3Client.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "putObject" -> {
                    if (failPuts) throw S3Exception.builder().message("AccessDenied").build();
                    calls.add(args[0]);
                    return null;
                }
                case "getObjectAsBytes" -> {
                    calls.add(args[0]);
                    return ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), storedBytes);
                }
                default -> throw new UnsupportedOperationException(method.getName());
            }
        });

    private final S3AttachmentStorage storage = new S3AttachmentStorage(s3, "keystone-photos");

    @Test
    void storeUploadsToTheConfiguredBucketUnderAPerWorkOrderKey() {
        UUID workOrderId = UUID.randomUUID();

        String key = storage.store(workOrderId, new byte[]{1, 2, 3, 4}, "image/png").orElseThrow();

        assertTrue(key.startsWith("work-orders/" + workOrderId + "/"), "key was " + key);
        PutObjectRequest sent = (PutObjectRequest) calls.get(0);
        assertEquals("keystone-photos", sent.bucket());
        assertEquals(key, sent.key());
        assertEquals("image/png", sent.contentType());
    }

    @Test
    void fetchReadsTheSameBucketAndKey() {
        storedBytes = new byte[]{9, 8, 7};

        assertArrayEquals(storedBytes, storage.fetch("work-orders/x/y"));

        GetObjectRequest sent = (GetObjectRequest) calls.get(0);
        assertEquals("keystone-photos", sent.bucket());
        assertEquals("work-orders/x/y", sent.key());
    }

    @Test
    void anS3FailureBecomesAStorageExceptionNotARawSdkError() {
        failPuts = true;

        assertThrows(StorageException.class, () -> storage.store(UUID.randomUUID(), new byte[]{1}, "image/png"));
    }
}
