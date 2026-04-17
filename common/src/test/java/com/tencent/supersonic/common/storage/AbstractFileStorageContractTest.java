package com.tencent.supersonic.common.storage;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractFileStorageContractTest {

    private static final String TODAY =
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    protected abstract FileStorage createStorage();

    protected abstract boolean supportsPresign();

    @Test
    void uploadThenDownloadRoundtrip() throws IOException {
        FileStorage storage = createStorage();
        String key = "exports/7/" + TODAY + "/42/hello.txt";
        byte[] payload = "hello supersonic".getBytes(StandardCharsets.UTF_8);
        storage.upload(key, new ByteArrayInputStream(payload), payload.length);
        assertTrue(storage.exists(key));
        try (InputStream in = storage.download(key)) {
            assertArrayEquals(payload, in.readAllBytes());
        }
    }

    @Test
    void deleteRemovesObject() {
        FileStorage storage = createStorage();
        String key = "exports/7/" + TODAY + "/42/to-delete.txt";
        byte[] payload = "bye".getBytes(StandardCharsets.UTF_8);
        storage.upload(key, new ByteArrayInputStream(payload), payload.length);
        assertTrue(storage.exists(key));
        storage.delete(key);
        assertFalse(storage.exists(key));
    }

    @Test
    void deleteMissingIsNoOp() {
        FileStorage storage = createStorage();
        storage.delete("exports/7/" + TODAY + "/999/does-not-exist.txt");
    }

    @Test
    void downloadMissingThrows() {
        FileStorage storage = createStorage();
        assertThrows(FileStorageException.class,
                () -> storage.download("exports/7/" + TODAY + "/999/missing.txt"));
    }

    @Test
    void existsFalseForMissing() {
        FileStorage storage = createStorage();
        assertFalse(storage.exists("exports/7/" + TODAY + "/999/missing.txt"));
    }

    @Test
    void tenantPrefixesAreIsolated() throws IOException {
        FileStorage storage = createStorage();
        String keyTenant7 = "exports/7/" + TODAY + "/1/file.txt";
        String keyTenant9 = "exports/9/" + TODAY + "/1/file.txt";
        byte[] payload7 = "tenant-7-data".getBytes(StandardCharsets.UTF_8);
        byte[] payload9 = "tenant-9-data".getBytes(StandardCharsets.UTF_8);
        storage.upload(keyTenant7, new ByteArrayInputStream(payload7), payload7.length);
        storage.upload(keyTenant9, new ByteArrayInputStream(payload9), payload9.length);
        try (InputStream in = storage.download(keyTenant7)) {
            assertArrayEquals(payload7, in.readAllBytes());
        }
        try (InputStream in = storage.download(keyTenant9)) {
            assertArrayEquals(payload9, in.readAllBytes());
        }
        assertEquals(Long.valueOf(7L), StoragePath.extractTenantId(keyTenant7));
        assertEquals(Long.valueOf(9L), StoragePath.extractTenantId(keyTenant9));
    }

    @Test
    void presignedUrlReturnsOrNullBasedOnSupport() {
        FileStorage storage = createStorage();
        String key = "exports/7/" + TODAY + "/42/presign.txt";
        byte[] payload = "presigned".getBytes(StandardCharsets.UTF_8);
        storage.upload(key, new ByteArrayInputStream(payload), payload.length);
        String url = storage.presignedUrl(key, Duration.ofMinutes(5));
        if (supportsPresign()) {
            assertNotNull(url, "presigned URL must not be null for cloud backends");
            assertTrue(url.startsWith("http"), "presigned URL must be absolute: " + url);
        } else {
            assertNull(url, "local backend must return null for presigned URL");
        }
    }

    @Test
    void storageTypeIsReported() {
        FileStorage storage = createStorage();
        String type = storage.getStorageType();
        assertNotNull(type);
        assertTrue(type.equals("local") || type.equals("oss") || type.equals("s3"),
                "unexpected storage type: " + type);
    }
}
