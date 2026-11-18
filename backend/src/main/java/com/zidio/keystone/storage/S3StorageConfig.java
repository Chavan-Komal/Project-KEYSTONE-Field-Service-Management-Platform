package com.zidio.keystone.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/** Only active when STORAGE_TYPE=s3. Fails fast at startup if the bucket is missing. */
@Configuration
@ConditionalOnProperty(name = "keystone.storage.type", havingValue = "s3")
public class S3StorageConfig {

    @Bean
    public AttachmentStorage attachmentStorage(
        @Value("${keystone.storage.s3.bucket:}") String bucket,
        @Value("${keystone.storage.s3.region:us-east-1}") String region,
        @Value("${keystone.storage.s3.endpoint:}") String endpoint,
        @Value("${keystone.storage.s3.access-key:}") String accessKey,
        @Value("${keystone.storage.s3.secret-key:}") String secretKey,
        @Value("${keystone.storage.s3.path-style:false}") boolean pathStyle
    ) {
        if (bucket.isBlank()) {
            throw new IllegalStateException("STORAGE_TYPE=s3 requires S3_BUCKET to be set.");
        }

        var builder = S3Client.builder()
            .region(Region.of(region))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build());

        // Explicit keys if given; otherwise the standard AWS chain (env vars, IAM role, ...).
        if (!accessKey.isBlank() && !secretKey.isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        // Non-AWS stores (R2, MinIO, Supabase) need their own endpoint.
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return new S3AttachmentStorage(builder.build(), bucket);
    }
}
