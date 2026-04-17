package com.tencent.supersonic.common.storage.s3;

import com.tencent.supersonic.common.storage.FileStorage;
import com.tencent.supersonic.common.storage.FileStorageException;
import com.tencent.supersonic.common.storage.StorageProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.ProxyConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Slf4j
public class S3FileStorage implements FileStorage {

    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;

    public S3FileStorage(StorageProperties properties) {
        StorageProperties.S3 cfg = properties.getS3();
        requireNonBlank(cfg.getRegion(), "s2.storage.s3.region");
        requireNonBlank(cfg.getBucket(), "s2.storage.s3.bucket");
        requireNonBlank(cfg.getAccessKey(), "s2.storage.s3.access-key");
        requireNonBlank(cfg.getSecretKey(), "s2.storage.s3.secret-key");

        StaticCredentialsProvider creds = StaticCredentialsProvider
                .create(AwsBasicCredentials.create(cfg.getAccessKey(), cfg.getSecretKey()));
        S3Configuration s3cfg =
                S3Configuration.builder().pathStyleAccessEnabled(cfg.isPathStyle()).build();

        var clientBuilder = S3Client.builder().region(Region.of(cfg.getRegion()))
                .credentialsProvider(creds).serviceConfiguration(s3cfg);
        var presignerBuilder = S3Presigner.builder().region(Region.of(cfg.getRegion()))
                .credentialsProvider(creds).serviceConfiguration(s3cfg);
        if (cfg.getEndpoint() != null && !cfg.getEndpoint().isBlank()) {
            URI uri = URI.create(cfg.getEndpoint());
            clientBuilder.endpointOverride(uri);
            presignerBuilder.endpointOverride(uri);
        }
        if (cfg.isNoProxy()) {
            var noProxyHttpClient = UrlConnectionHttpClient.builder()
                    .proxyConfiguration(
                            ProxyConfiguration.builder().useSystemPropertyValues(false).build())
                    .build();
            clientBuilder.httpClient(noProxyHttpClient);
        }
        this.client = clientBuilder.build();
        this.presigner = presignerBuilder.build();
        this.bucket = cfg.getBucket();
        log.info("S3FileStorage initialized for bucket={} region={}", bucket, cfg.getRegion());
    }

    @Override
    public void upload(String key, InputStream content, long sizeBytes) {
        try {
            RequestBody body = sizeBytes >= 0 ? RequestBody.fromInputStream(content, sizeBytes)
                    : RequestBody.fromBytes(content.readAllBytes());
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(), body);
        } catch (Exception e) {
            throw new FileStorageException("Failed to upload to S3: " + key, e);
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            return client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (NoSuchKeyException e) {
            throw new FileStorageException("File not found: " + key, e);
        } catch (Exception e) {
            throw new FileStorageException("Failed to download from S3: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception e) {
            log.warn("Failed to delete S3 object: {}", key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404)
                return false;
            throw new FileStorageException("Failed to stat S3 object: " + key, e);
        } catch (Exception e) {
            throw new FileStorageException("Failed to stat S3 object: " + key, e);
        }
    }

    @Override
    public String presignedUrl(String key, Duration ttl) {
        try {
            GetObjectPresignRequest presign =
                    GetObjectPresignRequest.builder().signatureDuration(ttl)
                            .getObjectRequest(
                                    GetObjectRequest.builder().bucket(bucket).key(key).build())
                            .build();
            return presigner.presignGetObject(presign).url().toString();
        } catch (Exception e) {
            throw new FileStorageException("Failed to presign S3 URL: " + key, e);
        }
    }

    @Override
    public String getStorageType() {
        return "s3";
    }

    @PreDestroy
    public void close() {
        if (presigner != null)
            presigner.close();
        if (client != null)
            client.close();
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank())
            throw new IllegalStateException(name + " is required when s2.storage.type=s3");
    }
}
