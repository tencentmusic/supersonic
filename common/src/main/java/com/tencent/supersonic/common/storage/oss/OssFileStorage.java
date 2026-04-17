package com.tencent.supersonic.common.storage.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.tencent.supersonic.common.storage.FileStorage;
import com.tencent.supersonic.common.storage.FileStorageException;
import com.tencent.supersonic.common.storage.StorageProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.time.Duration;
import java.util.Date;

@Slf4j
public class OssFileStorage implements FileStorage {

    private final OSS client;
    private final String bucket;

    public OssFileStorage(StorageProperties properties) {
        StorageProperties.Oss cfg = properties.getOss();
        requireNonBlank(cfg.getEndpoint(), "s2.storage.oss.endpoint");
        requireNonBlank(cfg.getBucket(), "s2.storage.oss.bucket");
        requireNonBlank(cfg.getAccessKeyId(), "s2.storage.oss.access-key-id");
        requireNonBlank(cfg.getAccessKeySecret(), "s2.storage.oss.access-key-secret");
        this.client = new OSSClientBuilder().build(cfg.getEndpoint(), cfg.getAccessKeyId(),
                cfg.getAccessKeySecret());
        this.bucket = cfg.getBucket();
        log.info("OssFileStorage initialized for bucket={} endpoint={}", bucket, cfg.getEndpoint());
    }

    @Override
    public void upload(String key, InputStream content, long sizeBytes) {
        try {
            ObjectMetadata meta = new ObjectMetadata();
            if (sizeBytes >= 0)
                meta.setContentLength(sizeBytes);
            client.putObject(bucket, key, content, meta);
        } catch (Exception e) {
            throw new FileStorageException("Failed to upload to OSS: " + key, e);
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            return client.getObject(bucket, key).getObjectContent();
        } catch (OSSException e) {
            if ("NoSuchKey".equals(e.getErrorCode())) {
                throw new FileStorageException("File not found: " + key, e);
            }
            throw new FileStorageException("Failed to download from OSS: " + key, e);
        } catch (Exception e) {
            throw new FileStorageException("Failed to download from OSS: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.deleteObject(bucket, key);
        } catch (Exception e) {
            log.warn("Failed to delete OSS object: {}", key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return client.doesObjectExist(bucket, key);
        } catch (Exception e) {
            throw new FileStorageException("Failed to stat OSS object: " + key, e);
        }
    }

    @Override
    public String presignedUrl(String key, Duration ttl) {
        try {
            Date expire = new Date(System.currentTimeMillis() + ttl.toMillis());
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(bucket, key);
            req.setExpiration(expire);
            return client.generatePresignedUrl(req).toString();
        } catch (Exception e) {
            throw new FileStorageException("Failed to presign OSS URL: " + key, e);
        }
    }

    @Override
    public String getStorageType() {
        return "oss";
    }

    @PreDestroy
    public void close() {
        if (client != null)
            client.shutdown();
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank())
            throw new IllegalStateException(name + " is required when s2.storage.type=oss");
    }
}
