package com.tencent.supersonic.common.storage.local;

import com.tencent.supersonic.common.storage.FileStorage;
import com.tencent.supersonic.common.storage.FileStorageException;
import com.tencent.supersonic.common.storage.StorageProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

@Slf4j
public class LocalFileStorage implements FileStorage {

    private final Path rootDir;

    public LocalFileStorage(StorageProperties properties) {
        this.rootDir = Paths.get(properties.getLocal().getRootDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new FileStorageException("Failed to create local storage root: " + rootDir, e);
        }
        log.info("LocalFileStorage initialized at {}", rootDir);
    }

    @Override
    public void upload(String key, InputStream content, long sizeBytes) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Failed to upload local file: " + key, e);
        }
    }

    @Override
    public InputStream download(String key) {
        Path target = resolve(key);
        if (!Files.exists(target)) {
            throw new FileStorageException("File not found: " + key);
        }
        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new FileStorageException("Failed to open local file: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        Path target = resolve(key);
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Failed to delete local file: {}", key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolve(key));
    }

    @Override
    public String presignedUrl(String key, Duration ttl) {
        return null;
    }

    @Override
    public String getStorageType() {
        return "local";
    }

    private Path resolve(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (key.startsWith("/") || key.contains("..")) {
            throw new IllegalArgumentException("invalid key: " + key);
        }
        Path resolved = rootDir.resolve(key).normalize();
        if (!resolved.startsWith(rootDir)) {
            throw new IllegalArgumentException("key escapes root: " + key);
        }
        return resolved;
    }
}
