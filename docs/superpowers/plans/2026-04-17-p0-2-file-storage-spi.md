# P0-2: FileStorage SPI (Local / OSS / S3) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract a pluggable `FileStorage` SPI so export/report artifacts can live on local disk, Aliyun OSS, or AWS S3 — making SuperSonic safe to run behind a load balancer with multiple instances.

**Architecture:** New module `common/src/main/java/com/tencent/supersonic/common/storage/` holds the interface + POJO + exception. Three concrete implementations (`LocalFileStorage`, `OssFileStorage`, `S3FileStorage`) register via `META-INF/spring.factories` and are selected by a Spring `@Configuration` that uses `@ConditionalOnProperty(name="s2.storage.type", havingValue=...)`. `DownloadServiceImpl`, `ExportTaskServiceImpl`, `ExportFileCleanupTask` are refactored to call `FileStorage` instead of raw `java.io.File`. Paths are namespaced `exports/{tenantId}/{yyyyMMdd}/{taskId}/{filename}` to prevent cross-tenant leakage. The existing `fileLocation` DB column continues to store the storage-relative key; the download endpoint translates it through the active `FileStorage` bean (direct stream for local, 302 to presigned URL for OSS/S3).

**Tech Stack:** Java 21, Spring Boot 3.4.11, Aliyun OSS Java SDK `3.17.4`, AWS SDK v2 `2.28.29`, MinIO Testcontainers `1.20.4` (tests), JUnit 5, Mockito.

---

## File Structure

**New files:**

```
common/src/main/java/com/tencent/supersonic/common/storage/
├── FileStorage.java                          # SPI interface
├── FileStorageException.java                 # Runtime exception
├── StoragePath.java                          # Helper for tenant-aware path building
├── StorageProperties.java                    # @ConfigurationProperties for s2.storage.*
├── FileStorageAutoConfiguration.java         # @Configuration with @ConditionalOnProperty
├── local/
│   └── LocalFileStorage.java
├── oss/
│   └── OssFileStorage.java
└── s3/
    └── S3FileStorage.java

common/src/test/java/com/tencent/supersonic/common/storage/
├── AbstractFileStorageContractTest.java      # Shared test suite for all impls
├── local/
│   └── LocalFileStorageTest.java
├── oss/
│   └── OssFileStorageTest.java               # MinIO via Testcontainers
└── s3/
    └── S3FileStorageTest.java                # MinIO via Testcontainers

common/src/main/resources/META-INF/spring.factories   # SPI registration

docs/runbook/file-storage-migration.md                # Ops guide + rollback
```

**Modified files:**

```
common/pom.xml                                # Add optional OSS + S3 + MinIO deps
common/src/main/java/com/tencent/supersonic/common/storage/... (above)
headless/server/pom.xml                       # No-op: consumes common
headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/DownloadServiceImpl.java
headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ExportTaskServiceImpl.java
headless/server/src/main/java/com/tencent/supersonic/headless/server/task/ExportFileCleanupTask.java
headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/ExportTaskController.java
launchers/standalone/src/main/resources/application.yaml       # Add s2.storage.* block
launchers/standalone/src/main/resources/META-INF/spring.factories  # Register auto-config
webapp/packages/supersonic-fe/src/services/exportTask.ts       # Follow 302 redirect for presigned URL
```

---

## Task 1: Define the `FileStorage` SPI + POJO + Exception

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/storage/FileStorage.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/storage/FileStorageException.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/storage/StoragePath.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/storage/StorageProperties.java`

- [ ] **Step 1: Create `FileStorageException`**

```java
package com.tencent.supersonic.common.storage;

/**
 * Thrown when a file storage operation fails. Always wraps the underlying provider exception
 * so callers have a single type to catch regardless of backend (local / OSS / S3).
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 2: Create `FileStorage` interface**

```java
package com.tencent.supersonic.common.storage;

import java.io.InputStream;
import java.time.Duration;

/**
 * Abstraction over blob storage backends used for export/report artifacts.
 *
 * <p>All paths are relative keys (no leading slash). Implementations MUST namespace the
 * physical storage location under a tenant-specific prefix. Use {@link StoragePath#forTenant}
 * to build keys.
 *
 * <p>Impls must be thread-safe; a single instance is shared across the Spring context.
 */
public interface FileStorage {

    /**
     * Upload bytes to the given key. Overwrites any existing object. The caller is responsible
     * for closing the stream.
     *
     * @param key      relative storage key, e.g. {@code exports/123/20260417/42/file.xlsx}
     * @param content  stream to upload
     * @param sizeBytes exact content length, or -1 if unknown
     */
    void upload(String key, InputStream content, long sizeBytes);

    /**
     * Open a read stream for the given key. Caller MUST close the returned stream.
     *
     * @throws FileStorageException if the key does not exist
     */
    InputStream download(String key);

    /**
     * Delete the object at the given key. No-op if the key does not exist.
     */
    void delete(String key);

    /**
     * Check whether an object exists at the given key.
     */
    boolean exists(String key);

    /**
     * Generate a short-lived URL allowing a browser to download the object directly, bypassing
     * backend bandwidth. For backends that do not support presigning (Local), impls return
     * {@code null} — callers MUST fall back to streaming via the backend endpoint.
     *
     * @param key the storage key
     * @param ttl how long the URL stays valid (max 7 days for S3)
     * @return a fully-qualified URL, or {@code null} if unsupported
     */
    String presignedUrl(String key, Duration ttl);

    /**
     * Report which backend this instance talks to. Used for logging and the runbook.
     * Values: {@code local}, {@code oss}, {@code s3}.
     */
    String getStorageType();
}
```

- [ ] **Step 3: Create `StoragePath`**

```java
package com.tencent.supersonic.common.storage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Builds tenant-namespaced storage keys so no impl can accidentally serve another tenant's file.
 *
 * <p>Format: {@code {prefix}/{tenantId}/{yyyyMMdd}/{groupId}/{fileName}}
 */
public final class StoragePath {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private StoragePath() {}

    public static String forTenant(String prefix, Long tenantId, Long groupId, String fileName) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required for storage key");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName is required");
        }
        String date = LocalDate.now().format(DATE_FMT);
        return String.format("%s/%d/%s/%d/%s", prefix, tenantId, date, groupId == null ? 0 : groupId,
                fileName);
    }

    /**
     * Extract the tenant id embedded in a key produced by {@link #forTenant}. Used by the
     * download endpoint to re-check tenant ownership.
     *
     * @return parsed tenant id, or {@code null} if the key is not in the expected shape
     */
    public static Long extractTenantId(String key) {
        if (key == null) {
            return null;
        }
        String[] parts = key.split("/");
        if (parts.length < 2) {
            return null;
        }
        try {
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
```

- [ ] **Step 4: Create `StorageProperties`**

```java
package com.tencent.supersonic.common.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code s2.storage.*} from application.yaml. A single properties class
 * holds both the selector ({@code type}) and the per-backend settings so tests can
 * instantiate it directly.
 */
@Data
@ConfigurationProperties(prefix = "s2.storage")
public class StorageProperties {

    /** {@code local}, {@code oss}, or {@code s3}. Default is local for backwards compatibility. */
    private String type = "local";

    /** Key prefix used by {@link StoragePath#forTenant}. */
    private String prefix = "exports";

    private Local local = new Local();
    private Oss oss = new Oss();
    private S3 s3 = new S3();

    @Data
    public static class Local {
        /** Filesystem root. Defaults to the java tmp dir. */
        private String rootDir = System.getProperty("java.io.tmpdir") + "/supersonic-export";
    }

    @Data
    public static class Oss {
        private String endpoint;
        private String bucket;
        private String accessKeyId;
        private String accessKeySecret;
    }

    @Data
    public static class S3 {
        private String endpoint;
        private String region;
        private String bucket;
        private String accessKey;
        private String secretKey;
        /** Path-style access (required for MinIO; leave false for real AWS). */
        private boolean pathStyle = false;
    }
}
```

- [ ] **Step 5: Compile**

Run: `mvn compile -pl common -am`
Expected: `BUILD SUCCESS` with no warnings about the new files.

- [ ] **Step 6: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/storage/FileStorage.java \
        common/src/main/java/com/tencent/supersonic/common/storage/FileStorageException.java \
        common/src/main/java/com/tencent/supersonic/common/storage/StoragePath.java \
        common/src/main/java/com/tencent/supersonic/common/storage/StorageProperties.java
git commit -m "feat(storage): define FileStorage SPI + tenant-aware path helper"
```

---

## Task 2: Abstract Contract Test Suite

**Files:**
- Create: `common/src/test/java/com/tencent/supersonic/common/storage/AbstractFileStorageContractTest.java`

- [ ] **Step 1: Write the abstract contract test**

```java
package com.tencent.supersonic.common.storage;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every {@link FileStorage} implementation MUST pass this suite. Extend and override
 * {@link #createStorage()} to return a freshly-configured impl pointing at an empty backend.
 */
public abstract class AbstractFileStorageContractTest {

    /** @return a fresh storage instance with an empty backend. */
    protected abstract FileStorage createStorage();

    /** @return true if the backend supports presigned URLs (OSS/S3 yes, Local no). */
    protected abstract boolean supportsPresign();

    @Test
    void uploadThenDownloadRoundtrip() throws IOException {
        FileStorage storage = createStorage();
        String key = "exports/7/20260417/42/hello.txt";
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
        String key = "exports/7/20260417/42/to-delete.txt";
        byte[] payload = "bye".getBytes(StandardCharsets.UTF_8);
        storage.upload(key, new ByteArrayInputStream(payload), payload.length);
        assertTrue(storage.exists(key));

        storage.delete(key);

        assertFalse(storage.exists(key));
    }

    @Test
    void deleteMissingIsNoOp() {
        FileStorage storage = createStorage();
        // Must not throw
        storage.delete("exports/7/20260417/999/does-not-exist.txt");
    }

    @Test
    void downloadMissingThrows() {
        FileStorage storage = createStorage();
        assertThrows(FileStorageException.class,
                () -> storage.download("exports/7/20260417/999/missing.txt"));
    }

    @Test
    void existsFalseForMissing() {
        FileStorage storage = createStorage();
        assertFalse(storage.exists("exports/7/20260417/999/missing.txt"));
    }

    @Test
    void tenantPrefixesAreIsolated() throws IOException {
        FileStorage storage = createStorage();
        String keyTenant7 = "exports/7/20260417/1/file.txt";
        String keyTenant9 = "exports/9/20260417/1/file.txt";
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
        String key = "exports/7/20260417/42/presign.txt";
        byte[] payload = "presigned".getBytes(StandardCharsets.UTF_8);
        storage.upload(key, new ByteArrayInputStream(payload), payload.length);

        String url = storage.presignedUrl(key, Duration.ofMinutes(5));

        if (supportsPresign()) {
            assertNotNull(url, "presigned URL must not be null for cloud backends");
            assertTrue(url.startsWith("http"), "presigned URL must be absolute: " + url);
        } else {
            assertEquals(null, url, "local backend must return null for presigned URL");
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
```

- [ ] **Step 2: Compile the tests**

Run: `mvn test-compile -pl common`
Expected: `BUILD SUCCESS`. No test runs yet — this is an abstract class with no extenders.

- [ ] **Step 3: Commit**

```bash
git add common/src/test/java/com/tencent/supersonic/common/storage/AbstractFileStorageContractTest.java
git commit -m "test(storage): add abstract contract test for FileStorage SPI"
```

---

## Task 3: Implement `LocalFileStorage`

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/storage/local/LocalFileStorage.java`
- Create: `common/src/test/java/com/tencent/supersonic/common/storage/local/LocalFileStorageTest.java`

- [ ] **Step 1: Write the concrete test that extends the contract**

```java
package com.tencent.supersonic.common.storage.local;

import com.tencent.supersonic.common.storage.AbstractFileStorageContractTest;
import com.tencent.supersonic.common.storage.FileStorage;
import com.tencent.supersonic.common.storage.StorageProperties;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

class LocalFileStorageTest extends AbstractFileStorageContractTest {

    @TempDir
    Path tempDir;

    @Override
    protected FileStorage createStorage() {
        StorageProperties props = new StorageProperties();
        props.setType("local");
        props.getLocal().setRootDir(tempDir.toString());
        return new LocalFileStorage(props);
    }

    @Override
    protected boolean supportsPresign() {
        return false;
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run: `mvn test -pl common -Dtest=LocalFileStorageTest`
Expected: FAIL — `cannot find symbol class LocalFileStorage`.

- [ ] **Step 3: Implement `LocalFileStorage`**

```java
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

/**
 * Local-filesystem implementation. Only suitable for single-instance deployments or
 * when the mount is a shared NFS across instances. Returns {@code null} for presigned URLs
 * so callers fall back to backend streaming.
 */
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
        // Local backend cannot serve HTTP directly — caller streams via backend endpoint.
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
        // Reject absolute keys / traversal.
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
```

- [ ] **Step 4: Run the test to confirm it passes**

Run: `mvn test -pl common -Dtest=LocalFileStorageTest`
Expected: `Tests run: 8, Failures: 0, Errors: 0` — all 8 contract tests pass.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/storage/local/LocalFileStorage.java \
        common/src/test/java/com/tencent/supersonic/common/storage/local/LocalFileStorageTest.java
git commit -m "feat(storage): implement LocalFileStorage with path-traversal guard"
```

---

## Task 4: Implement `OssFileStorage` (Aliyun)

**Files:**
- Modify: `common/pom.xml`
- Create: `common/src/main/java/com/tencent/supersonic/common/storage/oss/OssFileStorage.java`
- Create: `common/src/test/java/com/tencent/supersonic/common/storage/oss/OssFileStorageTest.java`

- [ ] **Step 1: Add OSS SDK + MinIO Testcontainers to `common/pom.xml`**

Open `common/pom.xml` and add the following entries inside `<dependencies>`, just before the closing `</dependencies>` tag (around line 254, after the `mockito-inline` block):

```xml
        <!-- FileStorage: Aliyun OSS (optional — only used when s2.storage.type=oss) -->
        <dependency>
            <groupId>com.aliyun.oss</groupId>
            <artifactId>aliyun-sdk-oss</artifactId>
            <version>3.17.4</version>
            <optional>true</optional>
        </dependency>
        <!-- FileStorage: AWS SDK v2 S3 (optional — only used when s2.storage.type=s3) -->
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>s3</artifactId>
            <version>2.28.29</version>
            <optional>true</optional>
        </dependency>
        <!-- FileStorage: MinIO-backed integration tests for OSS / S3 contract -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
            <version>1.20.4</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>1.20.4</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>minio</artifactId>
            <version>1.20.4</version>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 2: Sync dependencies**

Run: `mvn dependency:resolve -pl common`
Expected: `BUILD SUCCESS`; `aliyun-sdk-oss-3.17.4.jar` and `s3-2.28.29.jar` present in output.

- [ ] **Step 3: Write the concrete test**

```java
package com.tencent.supersonic.common.storage.oss;

import com.tencent.supersonic.common.storage.AbstractFileStorageContractTest;
import com.tencent.supersonic.common.storage.FileStorage;
import com.tencent.supersonic.common.storage.StorageProperties;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.CreateBucketRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class OssFileStorageTest extends AbstractFileStorageContractTest {

    @Container
    static MinIOContainer MINIO = new MinIOContainer("minio/minio:RELEASE.2024-09-13T20-26-02Z")
            .withUserName("minioadmin")
            .withPassword("minioadmin");

    private static final String BUCKET = "supersonic-test";

    @BeforeAll
    static void createBucket() {
        OSS client = new OSSClientBuilder().build(MINIO.getS3URL(), MINIO.getUserName(),
                MINIO.getPassword());
        try {
            if (!client.doesBucketExist(BUCKET)) {
                client.createBucket(new CreateBucketRequest(BUCKET));
            }
        } finally {
            client.shutdown();
        }
    }

    @AfterAll
    static void emptyBucket() {
        OSS client = new OSSClientBuilder().build(MINIO.getS3URL(), MINIO.getUserName(),
                MINIO.getPassword());
        try {
            client.listObjects(BUCKET).getObjectSummaries()
                    .forEach(o -> client.deleteObject(BUCKET, o.getKey()));
        } finally {
            client.shutdown();
        }
    }

    @Override
    protected FileStorage createStorage() {
        StorageProperties props = new StorageProperties();
        props.setType("oss");
        props.getOss().setEndpoint(MINIO.getS3URL());
        props.getOss().setBucket(BUCKET);
        props.getOss().setAccessKeyId(MINIO.getUserName());
        props.getOss().setAccessKeySecret(MINIO.getPassword());
        return new OssFileStorage(props);
    }

    @Override
    protected boolean supportsPresign() {
        return true;
    }
}
```

- [ ] **Step 4: Run the test to confirm it fails**

Run: `mvn test -pl common -Dtest=OssFileStorageTest`
Expected: FAIL — `cannot find symbol class OssFileStorage`.

- [ ] **Step 5: Implement `OssFileStorage`**

```java
package com.tencent.supersonic.common.storage.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
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

/**
 * Aliyun OSS backed {@link FileStorage}. Uses the official {@code aliyun-sdk-oss} SDK.
 * Bucket must already exist — we do NOT auto-create it (ops should provision via Terraform
 * with the right lifecycle rules).
 */
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
            if (sizeBytes >= 0) {
                meta.setContentLength(sizeBytes);
            }
            client.putObject(bucket, key, content, meta);
        } catch (Exception e) {
            throw new FileStorageException("Failed to upload to OSS: " + key, e);
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            if (!client.doesObjectExist(bucket, key)) {
                throw new FileStorageException("File not found: " + key);
            }
            return client.getObject(bucket, key).getObjectContent();
        } catch (FileStorageException e) {
            throw e;
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
        if (client != null) {
            client.shutdown();
        }
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required when s2.storage.type=oss");
        }
    }
}
```

- [ ] **Step 6: Run the test to confirm it passes**

Run: `mvn test -pl common -Dtest=OssFileStorageTest`
Expected: `Tests run: 8, Failures: 0, Errors: 0`. First run pulls the MinIO image (~90 MB).

- [ ] **Step 7: Commit**

```bash
git add common/pom.xml \
        common/src/main/java/com/tencent/supersonic/common/storage/oss/OssFileStorage.java \
        common/src/test/java/com/tencent/supersonic/common/storage/oss/OssFileStorageTest.java
git commit -m "feat(storage): add OssFileStorage with MinIO-backed contract test"
```

---

## Task 5: Implement `S3FileStorage` (AWS SDK v2)

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/storage/s3/S3FileStorage.java`
- Create: `common/src/test/java/com/tencent/supersonic/common/storage/s3/S3FileStorageTest.java`

- [ ] **Step 1: Write the concrete test**

```java
package com.tencent.supersonic.common.storage.s3;

import com.tencent.supersonic.common.storage.AbstractFileStorageContractTest;
import com.tencent.supersonic.common.storage.FileStorage;
import com.tencent.supersonic.common.storage.StorageProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

import java.net.URI;

@Testcontainers
class S3FileStorageTest extends AbstractFileStorageContractTest {

    @Container
    static MinIOContainer MINIO = new MinIOContainer("minio/minio:RELEASE.2024-09-13T20-26-02Z")
            .withUserName("minioadmin")
            .withPassword("minioadmin");

    private static final String BUCKET = "supersonic-test-s3";

    private static S3Client adminClient() {
        return S3Client.builder()
                .endpointOverride(URI.create(MINIO.getS3URL()))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(MINIO.getUserName(), MINIO.getPassword())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @BeforeAll
    static void createBucket() {
        try (S3Client s3 = adminClient()) {
            try {
                s3.headBucket(b -> b.bucket(BUCKET));
            } catch (NoSuchBucketException e) {
                s3.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
            } catch (Exception e) {
                s3.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
            }
        }
    }

    @AfterAll
    static void emptyBucket() {
        try (S3Client s3 = adminClient()) {
            var listed = s3.listObjectsV2(ListObjectsV2Request.builder().bucket(BUCKET).build());
            if (!listed.contents().isEmpty()) {
                var ids = listed.contents().stream()
                        .map(o -> ObjectIdentifier.builder().key(o.key()).build()).toList();
                s3.deleteObjects(DeleteObjectsRequest.builder().bucket(BUCKET)
                        .delete(Delete.builder().objects(ids).build()).build());
            }
        }
    }

    @Override
    protected FileStorage createStorage() {
        StorageProperties props = new StorageProperties();
        props.setType("s3");
        props.getS3().setEndpoint(MINIO.getS3URL());
        props.getS3().setRegion("us-east-1");
        props.getS3().setBucket(BUCKET);
        props.getS3().setAccessKey(MINIO.getUserName());
        props.getS3().setSecretKey(MINIO.getPassword());
        props.getS3().setPathStyle(true);
        return new S3FileStorage(props);
    }

    @Override
    protected boolean supportsPresign() {
        return true;
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run: `mvn test -pl common -Dtest=S3FileStorageTest`
Expected: FAIL — `cannot find symbol class S3FileStorage`.

- [ ] **Step 3: Implement `S3FileStorage`**

```java
package com.tencent.supersonic.common.storage.s3;

import com.tencent.supersonic.common.storage.FileStorage;
import com.tencent.supersonic.common.storage.FileStorageException;
import com.tencent.supersonic.common.storage.StorageProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

/**
 * AWS S3 backed {@link FileStorage} using AWS SDK v2. Supports path-style access for
 * MinIO and similar S3-compatible services.
 */
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

        StaticCredentialsProvider creds = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(cfg.getAccessKey(), cfg.getSecretKey()));
        S3Configuration s3cfg = S3Configuration.builder()
                .pathStyleAccessEnabled(cfg.isPathStyle()).build();

        var clientBuilder = S3Client.builder()
                .region(Region.of(cfg.getRegion()))
                .credentialsProvider(creds)
                .serviceConfiguration(s3cfg);
        var presignerBuilder = S3Presigner.builder()
                .region(Region.of(cfg.getRegion()))
                .credentialsProvider(creds)
                .serviceConfiguration(s3cfg);
        if (cfg.getEndpoint() != null && !cfg.getEndpoint().isBlank()) {
            URI uri = URI.create(cfg.getEndpoint());
            clientBuilder.endpointOverride(uri);
            presignerBuilder.endpointOverride(uri);
        }
        this.client = clientBuilder.build();
        this.presigner = presignerBuilder.build();
        this.bucket = cfg.getBucket();
        log.info("S3FileStorage initialized for bucket={} region={} endpoint={}", bucket,
                cfg.getRegion(), cfg.getEndpoint());
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
            if (e.statusCode() == 404) {
                return false;
            }
            throw new FileStorageException("Failed to stat S3 object: " + key, e);
        } catch (Exception e) {
            throw new FileStorageException("Failed to stat S3 object: " + key, e);
        }
    }

    @Override
    public String presignedUrl(String key, Duration ttl) {
        try {
            GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(key).build();
            GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
                    .signatureDuration(ttl).getObjectRequest(get).build();
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
        if (presigner != null) {
            presigner.close();
        }
        if (client != null) {
            client.close();
        }
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required when s2.storage.type=s3");
        }
    }
}
```

- [ ] **Step 4: Run the test to confirm it passes**

Run: `mvn test -pl common -Dtest=S3FileStorageTest`
Expected: `Tests run: 8, Failures: 0, Errors: 0`.

- [ ] **Step 5: Run all three impls together**

Run: `mvn test -pl common -Dtest=LocalFileStorageTest,OssFileStorageTest,S3FileStorageTest`
Expected: `Tests run: 24, Failures: 0, Errors: 0`.

- [ ] **Step 6: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/storage/s3/S3FileStorage.java \
        common/src/test/java/com/tencent/supersonic/common/storage/s3/S3FileStorageTest.java
git commit -m "feat(storage): add S3FileStorage using AWS SDK v2 with presigned URLs"
```

---

## Task 6: Spring Auto-Configuration + Property Wiring

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/storage/FileStorageAutoConfiguration.java`
- Create: `common/src/main/resources/META-INF/spring.factories`
- Modify: `launchers/standalone/src/main/resources/application.yaml`
- Modify: `launchers/standalone/src/main/resources/META-INF/spring.factories`

- [ ] **Step 1: Create `FileStorageAutoConfiguration`**

```java
package com.tencent.supersonic.common.storage;

import com.tencent.supersonic.common.storage.local.LocalFileStorage;
import com.tencent.supersonic.common.storage.oss.OssFileStorage;
import com.tencent.supersonic.common.storage.s3.S3FileStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selects a single {@link FileStorage} bean based on {@code s2.storage.type}. Uses
 * {@link ConditionalOnProperty} (not {@code @ConditionalOnBean}) per the project memo
 * that {@code @ConditionalOnBean} is unreliable against auto-configured beans.
 *
 * <p>Default {@code s2.storage.type=local} preserves existing single-instance behaviour.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class FileStorageAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(name = "s2.storage.type", havingValue = "local", matchIfMissing = true)
    static class LocalStorageConfig {
        @Bean
        @ConditionalOnMissingBean(FileStorage.class)
        public FileStorage localFileStorage(StorageProperties properties) {
            log.info("FileStorage: selecting LocalFileStorage (s2.storage.type=local)");
            return new LocalFileStorage(properties);
        }
    }

    @Configuration
    @ConditionalOnClass(name = "com.aliyun.oss.OSS")
    @ConditionalOnProperty(name = "s2.storage.type", havingValue = "oss")
    static class OssStorageConfig {
        @Bean
        @ConditionalOnMissingBean(FileStorage.class)
        public FileStorage ossFileStorage(StorageProperties properties) {
            log.info("FileStorage: selecting OssFileStorage (s2.storage.type=oss)");
            return new OssFileStorage(properties);
        }
    }

    @Configuration
    @ConditionalOnClass(name = "software.amazon.awssdk.services.s3.S3Client")
    @ConditionalOnProperty(name = "s2.storage.type", havingValue = "s3")
    static class S3StorageConfig {
        @Bean
        @ConditionalOnMissingBean(FileStorage.class)
        public FileStorage s3FileStorage(StorageProperties properties) {
            log.info("FileStorage: selecting S3FileStorage (s2.storage.type=s3)");
            return new S3FileStorage(properties);
        }
    }
}
```

- [ ] **Step 2: Create `common/src/main/resources/META-INF/spring.factories`**

```
# FileStorage auto-configuration
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.tencent.supersonic.common.storage.FileStorageAutoConfiguration
```

- [ ] **Step 3: Add `s2.storage.*` block to `launchers/standalone/src/main/resources/application.yaml`**

Locate the `# Export configuration` block (around line 235 — `s2.export:`) and replace the single-line `storage-type: local` with a dedicated top-level block immediately below `s2.export`:

Find:

```yaml
  # Export configuration
  export:
    worker-threads: 3
    max-queue-size: 50
    sync-threshold: 5000
    file-expire-days: 7
    storage-type: local
    local-dir: ${java.io.tmpdir}/supersonic-export
```

Replace with:

```yaml
  # Export configuration
  export:
    worker-threads: 3
    max-queue-size: 50
    sync-threshold: 5000
    file-expire-days: 7
  # Pluggable FileStorage backend (see docs/runbook/file-storage-migration.md)
  storage:
    type: ${S2_STORAGE_TYPE:local}
    prefix: exports
    local:
      root-dir: ${S2_STORAGE_LOCAL_DIR:${java.io.tmpdir}/supersonic-export}
    oss:
      endpoint: ${S2_STORAGE_OSS_ENDPOINT:}
      bucket: ${S2_STORAGE_OSS_BUCKET:}
      access-key-id: ${S2_STORAGE_OSS_AK:}
      access-key-secret: ${S2_STORAGE_OSS_SK:}
    s3:
      endpoint: ${S2_STORAGE_S3_ENDPOINT:}
      region: ${S2_STORAGE_S3_REGION:us-east-1}
      bucket: ${S2_STORAGE_S3_BUCKET:}
      access-key: ${S2_STORAGE_S3_AK:}
      secret-key: ${S2_STORAGE_S3_SK:}
      path-style: ${S2_STORAGE_S3_PATH_STYLE:false}
```

- [ ] **Step 4: Write a Spring slice test that boots the auto-config with `type=local`**

Create `common/src/test/java/com/tencent/supersonic/common/storage/FileStorageAutoConfigurationTest.java`:

```java
package com.tencent.supersonic.common.storage;

import com.tencent.supersonic.common.storage.local.LocalFileStorage;
import com.tencent.supersonic.common.storage.s3.S3FileStorage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FileStorageAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FileStorageAutoConfiguration.class));

    @Test
    void defaultsToLocal() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(FileStorage.class);
            assertThat(ctx.getBean(FileStorage.class)).isInstanceOf(LocalFileStorage.class);
        });
    }

    @Test
    void selectsLocalWhenConfigured() {
        runner.withPropertyValues("s2.storage.type=local").run(ctx -> {
            assertThat(ctx.getBean(FileStorage.class)).isInstanceOf(LocalFileStorage.class);
        });
    }

    @Test
    void selectsS3WhenConfigured() {
        runner.withPropertyValues(
                "s2.storage.type=s3",
                "s2.storage.s3.region=us-east-1",
                "s2.storage.s3.bucket=test",
                "s2.storage.s3.access-key=ak",
                "s2.storage.s3.secret-key=sk",
                "s2.storage.s3.path-style=true"
        ).run(ctx -> {
            assertThat(ctx.getBean(FileStorage.class)).isInstanceOf(S3FileStorage.class);
        });
    }
}
```

- [ ] **Step 5: Run the auto-config test**

Run: `mvn test -pl common -Dtest=FileStorageAutoConfigurationTest`
Expected: `Tests run: 3, Failures: 0, Errors: 0`.

- [ ] **Step 6: Confirm the launcher still compiles**

Run: `mvn compile -pl launchers/standalone -am`
Expected: `BUILD SUCCESS`. No changes needed to the launcher's own `spring.factories`; the auto-config loads via `common/src/main/resources/META-INF/spring.factories` automatically.

- [ ] **Step 7: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/storage/FileStorageAutoConfiguration.java \
        common/src/main/resources/META-INF/spring.factories \
        common/src/test/java/com/tencent/supersonic/common/storage/FileStorageAutoConfigurationTest.java \
        launchers/standalone/src/main/resources/application.yaml
git commit -m "feat(storage): wire FileStorage SPI via @ConditionalOnProperty"
```

---

## Task 7: Migrate `DownloadServiceImpl` to `FileStorage`

**Files:**
- Modify: `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/DownloadServiceImpl.java`

The current flow: write an `xlsx` to a `FileUtils.createTmpFile` location, `readFileToByteArray`, stream to `HttpServletResponse`. We keep the tmp-file pattern (EasyExcel needs a local file handle) but stream the final bytes through `FileStorage` for any caller that wants a persistent copy. For sync downloads, we still stream to the client directly — no storage round-trip — but we expose an injected `FileStorage` for the async path to use.

This task is a refactor that is API-stable (no signature changes), so its "test" is compilation plus the existing tests (there are no dedicated unit tests for `DownloadServiceImpl`).

- [ ] **Step 1: Replace the whole file with the new version**

Overwrite `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/DownloadServiceImpl.java`:

```java
package com.tencent.supersonic.headless.server.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.util.FileUtils;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.*;
import com.tencent.supersonic.common.storage.FileStorage;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.headless.api.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.api.pojo.DrillDownDimension;
import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import com.tencent.supersonic.headless.api.pojo.RelateDimension;
import com.tencent.supersonic.headless.api.pojo.enums.SemanticType;
import com.tencent.supersonic.headless.api.pojo.request.BatchDownloadReq;
import com.tencent.supersonic.headless.api.pojo.request.DownloadMetricReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.utils.DataTransformUtils;
import com.tencent.supersonic.headless.server.pojo.DataDownload;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.DownloadService;
import com.tencent.supersonic.headless.server.service.MetricService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadServiceImpl implements DownloadService {

    private static final String internMetricCol = "指标名称";
    private static final long downloadLimit = Constants.DEFAULT_DOWNLOAD_LIMIT;
    private static final String dateFormat = "yyyyMMddHHmmss";

    private final MetricService metricService;
    private final DimensionService dimensionService;
    private final SemanticLayerService queryService;
    private final FileStorage fileStorage;

    @Override
    public void downloadByStruct(DownloadMetricReq downloadMetricReq, User user,
            HttpServletResponse response) throws Exception {
        String fileName =
                String.format("%s_%s.xlsx", "supersonic", DateUtils.format(new Date(), dateFormat));
        File file = FileUtils.createTmpFile(fileName);
        try {
            QueryStructReq queryStructReq = metricService.convert(downloadMetricReq);
            SemanticQueryResp queryResult =
                    queryService.queryByReq(queryStructReq.convert(true), user);
            DataDownload dataDownload =
                    buildDataDownload(queryResult, queryStructReq, downloadMetricReq.isTransform());
            EasyExcel.write(file).sheet("Sheet1").head(dataDownload.getHeaders())
                    .doWrite(dataDownload.getData());
        } catch (RuntimeException e) {
            EasyExcel.write(file).sheet("Sheet1").head(buildErrMessageHead())
                    .doWrite(buildErrMessageData(e.getMessage()));
            return;
        }
        streamFileToResponse(response, file, fileName);
    }

    @Override
    public void batchDownload(BatchDownloadReq batchDownloadReq, User user,
            HttpServletResponse response) throws Exception {
        String fileName =
                String.format("%s_%s.xlsx", "supersonic", DateUtils.format(new Date(), dateFormat));
        File file = FileUtils.createTmpFile(fileName);
        List<Long> metricIds = batchDownloadReq.getMetricIds();
        if (CollectionUtils.isEmpty(metricIds)) {
            return;
        }
        batchDownload(batchDownloadReq, user, file);
        streamFileToResponse(response, file, fileName);
    }

    public void batchDownload(BatchDownloadReq batchDownloadReq, User user, File file)
            throws Exception {
        List<Long> metricIds = batchDownloadReq.getMetricIds();
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setIds(metricIds);
        List<MetricResp> metricResps = metricService.getMetrics(metaFilter);
        Map<String, List<MetricResp>> metricMap = getMetricMap(metricResps);
        Map<Long, List<DrillDownDimension>> drillDownMap =
                metricService.getDrillDownDimensionBatch(metricResps);
        List<Long> dimensionIds = drillDownMap.values().stream().flatMap(Collection::stream)
                .map(DrillDownDimension::getDimensionId).collect(Collectors.toList());
        metaFilter.setIds(dimensionIds);
        Map<Long, DimensionResp> dimensionRespMap = dimensionService.getDimensions(metaFilter)
                .stream().collect(Collectors.toMap(DimensionResp::getId, d -> d));
        ExcelWriter excelWriter = EasyExcel.write(file).build();
        int sheetCount = 1;
        for (List<MetricResp> metrics : metricMap.values()) {
            if (CollectionUtils.isEmpty(metrics)) {
                continue;
            }
            MetricResp metricResp = metrics.getFirst();
            List<DimensionResp> dimensions = getMetricRelaDimensions(metricResp, dimensionRespMap);
            for (MetricResp metric : metrics) {
                try {
                    QueryStructReq queryStructReq =
                            buildDownloadReq(dimensions, metric, batchDownloadReq);
                    QuerySqlReq querySqlReq = queryStructReq.convert();
                    querySqlReq.setNeedAuth(true);
                    SemanticQueryResp queryResult = queryService.queryByReq(querySqlReq, user);
                    DataDownload dataDownload = buildDataDownload(queryResult, queryStructReq,
                            batchDownloadReq.isTransform());
                    WriteSheet writeSheet = EasyExcel.writerSheet("Sheet" + sheetCount)
                            .head(dataDownload.getHeaders()).build();
                    excelWriter.write(dataDownload.getData(), writeSheet);
                } catch (RuntimeException e) {
                    EasyExcel.write(file).sheet("Sheet1").head(buildErrMessageHead())
                            .doWrite(buildErrMessageData(e.getMessage()));
                    return;
                }
            }
            sheetCount++;
        }
        excelWriter.finish();
    }

    private List<List<String>> buildErrMessageHead() {
        List<List<String>> headers = Lists.newArrayList();
        headers.add(Lists.newArrayList("异常提示"));
        return headers;
    }

    private List<List<String>> buildErrMessageData(String errMsg) {
        List<List<String>> data = Lists.newArrayList();
        data.add(Lists.newArrayList(errMsg));
        return data;
    }

    private List<List<String>> buildHeader(SemanticQueryResp semanticQueryResp) {
        List<List<String>> header = Lists.newArrayList();
        for (QueryColumn column : semanticQueryResp.getColumns()) {
            header.add(Lists.newArrayList(column.getName()));
        }
        return header;
    }

    private List<List<String>> buildHeader(List<QueryColumn> queryColumns, List<String> dateList) {
        List<List<String>> headers = Lists.newArrayList();
        for (QueryColumn queryColumn : queryColumns) {
            if (SemanticType.DATE.name().equals(queryColumn.getShowType())) {
                continue;
            }
            headers.add(Lists.newArrayList(queryColumn.getName()));
        }
        for (String date : dateList) {
            headers.add(Lists.newArrayList(date));
        }
        headers.add(Lists.newArrayList(internMetricCol));
        return headers;
    }

    private List<List<String>> buildData(SemanticQueryResp semanticQueryResp) {
        List<List<String>> data = new ArrayList<>();
        for (Map<String, Object> row : semanticQueryResp.getResultList()) {
            List<String> rowData = new ArrayList<>();
            for (QueryColumn column : semanticQueryResp.getColumns()) {
                rowData.add(String.valueOf(row.get(column.getBizName())));
            }
            data.add(rowData);
        }
        return data;
    }

    private List<List<String>> buildData(List<List<String>> headers, Map<String, String> nameMap,
            List<Map<String, Object>> dataTransformed, String metricName) {
        List<List<String>> data = Lists.newArrayList();
        for (Map<String, Object> map : dataTransformed) {
            List<String> row = Lists.newArrayList();
            for (List<String> header : headers) {
                String head = header.getFirst();
                if (internMetricCol.equals(head)) {
                    continue;
                }
                Object object = map.getOrDefault(nameMap.getOrDefault(head, head), "");
                if (object == null) {
                    row.add("");
                } else {
                    row.add(String.valueOf(object));
                }
            }
            row.add(metricName);
            data.add(row);
        }
        return data;
    }

    private DataDownload buildDataDownload(SemanticQueryResp queryResult,
            QueryStructReq queryStructReq, boolean isTransform) {
        List<QueryColumn> metricColumns = queryResult.getMetricColumns();
        List<QueryColumn> dimensionColumns = queryResult.getDimensionColumns();
        if (isTransform && !CollectionUtils.isEmpty(metricColumns)) {
            QueryColumn metric = metricColumns.getFirst();
            List<String> groups = queryStructReq.getGroups();
            List<Map<String, Object>> dataTransformed =
                    DataTransformUtils.transform(queryResult.getResultList(), metric.getBizName(),
                            groups, queryStructReq.getDateInfo());
            List<List<String>> headers =
                    buildHeader(dimensionColumns, queryStructReq.getDateInfo().getDateList());
            List<List<String>> data = buildData(headers, getDimensionNameMap(dimensionColumns),
                    dataTransformed, metric.getName());
            return DataDownload.builder().headers(headers).data(data).build();
        } else {
            List<List<String>> data = buildData(queryResult);
            List<List<String>> header = buildHeader(queryResult);
            return DataDownload.builder().data(data).headers(header).build();
        }
    }

    private QueryStructReq buildDownloadReq(List<DimensionResp> dimensionResps,
            MetricResp metricResp, BatchDownloadReq batchDownloadReq) {
        DateConf dateConf = batchDownloadReq.getDateInfo();
        Set<Long> modelIds =
                dimensionResps.stream().map(DimensionResp::getModelId).collect(Collectors.toSet());
        modelIds.add(metricResp.getModelId());
        QueryStructReq queryStructReq = new QueryStructReq();
        queryStructReq.setGroups(dimensionResps.stream().map(DimensionResp::getBizName)
                .collect(Collectors.toList()));
        queryStructReq.getGroups().addFirst(dateConf.getDateField());
        Aggregator aggregator = new Aggregator();
        aggregator.setColumn(metricResp.getBizName());
        queryStructReq.setAggregators(Lists.newArrayList(aggregator));
        queryStructReq.setDateInfo(dateConf);
        queryStructReq.setModelIds(modelIds);
        queryStructReq.setLimit(downloadLimit);
        return queryStructReq;
    }

    private Map<String, List<MetricResp>> getMetricMap(List<MetricResp> metricResps) {
        Map<Long, List<DrillDownDimension>> drillDownMap =
                metricService.getDrillDownDimensionBatch(metricResps);
        for (MetricResp metricResp : metricResps) {
            List<DrillDownDimension> drillDownDimensions =
                    drillDownMap.getOrDefault(metricResp.getId(), Collections.emptyList());
            RelateDimension relateDimension =
                    RelateDimension.builder().drillDownDimensions(drillDownDimensions).build();
            metricResp.setRelateDimension(relateDimension);
        }
        return metricResps.stream()
                .collect(Collectors.groupingBy(MetricResp::getRelaDimensionIdKey));
    }

    private Map<String, String> getDimensionNameMap(List<QueryColumn> queryColumns) {
        return queryColumns.stream()
                .collect(Collectors.toMap(QueryColumn::getName, QueryColumn::getBizName));
    }

    private List<DimensionResp> getMetricRelaDimensions(MetricResp metricResp,
            Map<Long, DimensionResp> dimensionRespMap) {
        if (metricResp.getRelateDimension() == null || CollectionUtils
                .isEmpty(metricResp.getRelateDimension().getDrillDownDimensions())) {
            return Lists.newArrayList();
        }
        return metricResp.getRelateDimension().getDrillDownDimensions().stream().map(
                drillDownDimension -> dimensionRespMap.get(drillDownDimension.getDimensionId()))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Streams the generated tmp file to the caller and deletes it afterwards. The file
     * is NOT persisted in {@link FileStorage} — sync downloads are one-shot. The
     * {@code fileStorage} dependency is still injected because async batch jobs invoke
     * this service and expect the bean to be present.
     */
    private void streamFileToResponse(HttpServletResponse response, File file, String filename) {
        try {
            response.reset();
            response.setCharacterEncoding("UTF-8");
            response.addHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
            response.addHeader("Content-Length", "" + file.length());
            response.setContentType("application/octet-stream");
            try (InputStream in = Files.newInputStream(file.toPath());
                    BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream())) {
                in.transferTo(out);
                out.flush();
            }
            // Reference the injected fileStorage so Spring resolves it and so we can later
            // persist batch exports without another refactor.
            if (log.isDebugEnabled()) {
                log.debug("Downloaded sync file via storage backend: {}",
                        fileStorage.getStorageType());
            }
        } catch (Exception e) {
            log.error("failed to download file", e);
        } finally {
            if (!file.delete()) {
                log.debug("tmp download file not deleted: {}", file.getAbsolutePath());
            }
        }
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn compile -pl headless/server -am`
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run existing headless-server tests to confirm no regression**

Run: `mvn test -pl headless/server -Dtest=ReportBaselineTest`
Expected: All existing tests pass.

- [ ] **Step 4: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/DownloadServiceImpl.java
git commit -m "refactor(download): inject FileStorage into DownloadServiceImpl"
```

---

## Task 8: Migrate `ExportTaskServiceImpl` + `ExportFileCleanupTask` to use `FileStorage`

**Files:**
- Modify: `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ExportTaskServiceImpl.java`
- Modify: `headless/server/src/main/java/com/tencent/supersonic/headless/server/task/ExportFileCleanupTask.java`
- Modify: `headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/ExportTaskController.java`

### 8a: `ExportTaskServiceImpl` — write to `FileStorage` instead of raw disk

Critical semantic change: the DB column `file_location` now stores a **storage key** (e.g. `exports/7/20260417/42/export_42_20260417103000.xlsx`), not an absolute filesystem path. The download controller (Task 8c) resolves the key through `FileStorage`.

- [ ] **Step 1: Apply the edit**

In `ExportTaskServiceImpl.java`:

1. Remove the `@Value("${supersonic.export.local-dir:...}")` field (lines 58-59 in current file).
2. Remove unused imports: `java.io.BufferedWriter`, `java.io.FileWriter` (replaced by stream-based writing).
3. Add imports:

```java
import com.tencent.supersonic.common.storage.FileStorage;
import com.tencent.supersonic.common.storage.StoragePath;
import com.tencent.supersonic.common.storage.StorageProperties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
```

4. Inject `FileStorage` and `StorageProperties` via the existing constructor. Replace:

```java
    public ExportTaskServiceImpl(@Qualifier("exportExecutor") ThreadPoolExecutor exportExecutor,
            SemanticLayerService semanticLayerService, RowCountEstimator rowCountEstimator,
            UserService userService, DataSetService dataSetService) {
        this.exportExecutor = exportExecutor;
        this.semanticLayerService = semanticLayerService;
        this.rowCountEstimator = rowCountEstimator;
        this.userService = userService;
        this.dataSetService = dataSetService;
    }
```

With:

```java
    private final FileStorage fileStorage;
    private final StorageProperties storageProperties;

    public ExportTaskServiceImpl(@Qualifier("exportExecutor") ThreadPoolExecutor exportExecutor,
            SemanticLayerService semanticLayerService, RowCountEstimator rowCountEstimator,
            UserService userService, DataSetService dataSetService,
            FileStorage fileStorage, StorageProperties storageProperties) {
        this.exportExecutor = exportExecutor;
        this.semanticLayerService = semanticLayerService;
        this.rowCountEstimator = rowCountEstimator;
        this.userService = userService;
        this.dataSetService = dataSetService;
        this.fileStorage = fileStorage;
        this.storageProperties = storageProperties;
    }
```

5. Replace `writeOutputFile(...)`, `writeExcel(...)`, `writeCsv(...)` with:

```java
    /**
     * Generate the export content in memory, upload it to {@link FileStorage}, and
     * return the storage key. In-memory buffering is acceptable because the async
     * threshold already bounded the row count (see {@link #shouldExecuteAsync}).
     */
    private String writeOutputToStorage(ExportTaskDO task, SemanticQueryResp queryResp)
            throws Exception {
        String timestamp = DateUtils.format(new Date(), "yyyyMMddHHmmss");
        boolean isCsv = "CSV".equalsIgnoreCase(task.getOutputFormat());
        String fileName = String.format("export_%d_%s.%s", task.getId(), timestamp,
                isCsv ? "csv" : "xlsx");

        byte[] bytes;
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            if (isCsv) {
                writeCsv(buffer, queryResp);
            } else {
                writeExcel(buffer, queryResp);
            }
            bytes = buffer.toByteArray();
        }

        String key = StoragePath.forTenant(storageProperties.getPrefix(), task.getTenantId(),
                task.getId(), fileName);
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            fileStorage.upload(key, in, bytes.length);
        }
        // Record the content length for the UI's "File size" column.
        task.setFileSize((long) bytes.length);
        return key;
    }

    private void writeExcel(ByteArrayOutputStream out, SemanticQueryResp queryResp) {
        List<List<String>> headers = buildHeaders(queryResp.getColumns());
        List<List<String>> data = buildData(queryResp);
        EasyExcel.write(out).sheet("Sheet1").head(headers).doWrite(data);
    }

    private void writeCsv(ByteArrayOutputStream out, SemanticQueryResp queryResp) throws Exception {
        try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            List<QueryColumn> columns = queryResp.getColumns();
            writer.write(columns.stream().map(QueryColumn::getName)
                    .collect(Collectors.joining(",")));
            writer.write("\n");
            if (queryResp.getResultList() != null) {
                for (Map<String, Object> row : queryResp.getResultList()) {
                    writer.write(columns.stream()
                            .map(col -> escapeCsv(row.get(col.getBizName())))
                            .collect(Collectors.joining(",")));
                    writer.write("\n");
                }
            }
        }
    }
```

6. Update `executeExport(Long taskId)` body — replace the lines that persisted the file location using `outputFile.length()` and `outputFile.getAbsolutePath()`:

Find:

```java
            // 4. Write to file
            File outputFile = writeOutputFile(task, queryResp);

            // 5. Update task with result
            task.setStatus(ExportTaskStatus.SUCCESS.name());
            task.setRowCount(
                    queryResp.getResultList() != null ? (long) queryResp.getResultList().size()
                            : 0L);
            task.setFileSize(outputFile.length());
            task.setFileLocation(outputFile.getAbsolutePath());
            baseMapper.updateById(task);
```

Replace with:

```java
            // 4. Write to storage
            String storageKey = writeOutputToStorage(task, queryResp);

            // 5. Update task with result
            task.setStatus(ExportTaskStatus.SUCCESS.name());
            task.setRowCount(
                    queryResp.getResultList() != null ? (long) queryResp.getResultList().size()
                            : 0L);
            // fileSize was set inside writeOutputToStorage
            task.setFileLocation(storageKey);
            baseMapper.updateById(task);
```

7. Delete the now-unused `java.io.File` import if no longer referenced (check after the edits).

- [ ] **Step 2: Compile**

Run: `mvn compile -pl headless/server -am`
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run the existing `ExportTaskServiceImplTest`**

Run: `mvn test -pl headless/server -Dtest=ExportTaskServiceImplTest`
Expected: The test currently asserts `Files.exists(Path.of(t.getFileLocation()))` which WILL FAIL after this change because `fileLocation` is a storage key, not an absolute path.

Update the test's assertions as follows in `headless/server/src/test/java/com/tencent/supersonic/headless/server/service/ExportTaskServiceImplTest.java`:

Find:

```java
                        && t.getFileLocation() != null
                        && java.nio.file.Files.exists(java.nio.file.Path.of(t.getFileLocation()))));
```

Replace with:

```java
                        && t.getFileLocation() != null
                        && t.getFileLocation().startsWith("exports/")));
```

Also: the test's `@SpringBootTest` will need a `FileStorage` bean. Since the auto-configuration defaults to `LocalFileStorage` and `StorageProperties` is picked up via `@EnableConfigurationProperties`, no further test-side wiring is needed — confirm by running the test again.

Run: `mvn test -pl headless/server -Dtest=ExportTaskServiceImplTest`
Expected: `Tests run: N, Failures: 0, Errors: 0`.

### 8b: `ExportFileCleanupTask` — delete via `FileStorage`

- [ ] **Step 4: Replace the cleanup task**

Overwrite `headless/server/src/main/java/com/tencent/supersonic/headless/server/task/ExportFileCleanupTask.java`:

```java
package com.tencent.supersonic.headless.server.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.common.storage.FileStorage;
import com.tencent.supersonic.headless.server.persistence.dataobject.ExportTaskDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ExportTaskMapper;
import com.tencent.supersonic.headless.server.pojo.ExportTaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExportFileCleanupTask {

    private final ExportTaskMapper exportTaskMapper;
    private final FileStorage fileStorage;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredExportFiles() {
        QueryWrapper<ExportTaskDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ExportTaskDO::getStatus, ExportTaskStatus.SUCCESS.name())
                .lt(ExportTaskDO::getExpireTime, new Date())
                .isNotNull(ExportTaskDO::getFileLocation);

        List<ExportTaskDO> expiredTasks = exportTaskMapper.selectList(wrapper);
        for (ExportTaskDO task : expiredTasks) {
            try {
                fileStorage.delete(task.getFileLocation());
                log.info("Deleted expired export file: key={} backend={}",
                        task.getFileLocation(), fileStorage.getStorageType());
            } catch (RuntimeException e) {
                log.warn("Failed to delete export file: {}", task.getFileLocation(), e);
            }
            task.setStatus(ExportTaskStatus.EXPIRED.name());
            exportTaskMapper.updateById(task);
        }
        if (!expiredTasks.isEmpty()) {
            log.info("Cleaned up {} expired export tasks", expiredTasks.size());
        }
    }
}
```

### 8c: `ExportTaskController` — support presigned-redirect or direct stream

- [ ] **Step 5: Replace the download endpoint**

Overwrite `headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/ExportTaskController.java`:

```java
package com.tencent.supersonic.headless.server.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.storage.FileStorage;
import com.tencent.supersonic.common.storage.StoragePath;
import com.tencent.supersonic.headless.server.persistence.dataobject.ExportTaskDO;
import com.tencent.supersonic.headless.server.service.ExportTaskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/exportTasks")
@Slf4j
@RequiredArgsConstructor
public class ExportTaskController {

    private final ExportTaskService exportTaskService;
    private final FileStorage fileStorage;

    @PostMapping
    public ExportTaskDO submitExportTask(@RequestBody ExportTaskDO task, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        task.setUserId(user.getId());
        task.setTenantId(user.getTenantId());
        return exportTaskService.submitExportTask(task);
    }

    @GetMapping
    public Page<ExportTaskDO> getTaskList(@RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int pageSize, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return exportTaskService.getTaskList(new Page<>(current, pageSize), user.getId());
    }

    @GetMapping("/{id}")
    public ExportTaskDO getTaskById(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        return exportTaskService.getTaskById(id);
    }

    @GetMapping("/{id}:download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        String key = exportTaskService.getDownloadPath(id);

        // Tenant check — the key's embedded tenant must match the caller's tenant.
        Long keyTenant = StoragePath.extractTenantId(key);
        if (user.getTenantId() != null && keyTenant != null
                && !keyTenant.equals(user.getTenantId())) {
            log.warn("Tenant mismatch on download: callerTenant={} keyTenant={} taskId={}",
                    user.getTenantId(), keyTenant, id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!fileStorage.exists(key)) {
            return ResponseEntity.notFound().build();
        }

        // Cloud backends: 302 to presigned URL so the browser pulls the bytes directly.
        String presigned = fileStorage.presignedUrl(key, Duration.ofMinutes(15));
        if (presigned != null) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(presigned)).build();
        }

        // Local backend: stream through the controller.
        String filename = key.substring(key.lastIndexOf('/') + 1);
        InputStream in = fileStorage.download(key);
        Resource resource = new InputStreamResource(in);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public void cancelTask(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        exportTaskService.cancelTask(id);
    }
}
```

- [ ] **Step 6: Compile**

Run: `mvn compile -pl launchers/standalone -am`
Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Run headless-server tests**

Run: `mvn test -pl headless/server`
Expected: All tests pass. If `FeishuDeliveryChannelTest` fails because it asserts a `/tmp/report.xlsx` literal, no change is needed there — the delivery channel still reads through whatever path the orchestrator handed it (not yet migrated; see roadmap for follow-up).

- [ ] **Step 8: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ExportTaskServiceImpl.java \
        headless/server/src/main/java/com/tencent/supersonic/headless/server/task/ExportFileCleanupTask.java \
        headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/ExportTaskController.java \
        headless/server/src/test/java/com/tencent/supersonic/headless/server/service/ExportTaskServiceImplTest.java
git commit -m "refactor(export): persist export artifacts through FileStorage SPI"
```

---

## Task 9: Frontend — follow presigned-URL redirect

**Files:**
- Modify: `webapp/packages/supersonic-fe/src/services/exportTask.ts`

The existing `downloadExportFile` fetches as `responseType: 'blob'` and triggers an `<a>` click. When the backend returns `302` with `Location: <presigned-url>`, `umi-request` (based on `fetch`) follows the redirect automatically in the browser — no code change needed for the happy path. But two things must be handled:

1. The redirect target is a different origin (OSS/S3). `umi-request` must NOT send our auth header to the third-party host.
2. We should prefer `window.location.href = redirectUrl` over the XHR-blob path for large cloud downloads so the browser manages the stream.

- [ ] **Step 1: Replace `downloadExportFile`**

Find the entire `downloadExportFile` function in `webapp/packages/supersonic-fe/src/services/exportTask.ts` and replace with:

```ts
export async function downloadExportFile(id: number, fallbackName?: string) {
  // Probe the endpoint first WITHOUT following redirects. If the backend returns 302,
  // we navigate the browser directly to the presigned URL (bypasses our bandwidth).
  // For local storage (no 302) we fall back to the blob-download flow.
  const probeUrl = `${BASE}/${id}:download`;

  // `redirect: 'manual'` tells fetch not to transparently follow the 302.
  // umi-request exposes the raw response when getResponse=true, so we use bare fetch here.
  const probeResponse = await fetch(probeUrl, {
    method: 'GET',
    credentials: 'include',
    redirect: 'manual',
  });

  // A manual-redirect fetch returns type 'opaqueredirect' and status 0 when the server
  // returned 3xx. In that case, just navigate there — the browser will stream the
  // presigned URL directly from OSS/S3.
  if (probeResponse.type === 'opaqueredirect' || (probeResponse.status >= 300 && probeResponse.status < 400)) {
    // We cannot read the Location header under 'manual' mode, so fall back to a fresh
    // navigation — the backend will issue the same 302 and the browser will follow it.
    window.location.href = probeUrl;
    return;
  }

  if (!probeResponse.ok) {
    throw new Error(`Download failed: ${probeResponse.status}`);
  }

  // Local backend: stream the bytes as a blob and save via <a> click.
  const blob = await probeResponse.blob();
  const contentDisposition = probeResponse.headers.get('content-disposition');
  const fileName = parseFilenameFromContentDisposition(contentDisposition, fallbackName);
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
}
```

- [ ] **Step 2: Manually smoke-test the flow**

Run the frontend dev server and:

```bash
cd webapp/packages/supersonic-fe && npm run start
```

Navigate to TaskCenter → Export tab → click 下载 on a SUCCESS row. With `s2.storage.type=local`, the blob path is used. With `s2.storage.type=oss` or `s3`, the window navigates to the presigned URL.

- [ ] **Step 3: Commit**

```bash
git add webapp/packages/supersonic-fe/src/services/exportTask.ts
git commit -m "feat(webapp): follow presigned-URL redirect for export download"
```

---

## Task 10: Runbook + Rollback Strategy

**Files:**
- Create: `docs/runbook/file-storage-migration.md`

- [ ] **Step 1: Write the runbook**

Create `docs/runbook/file-storage-migration.md`:

```markdown
# FileStorage Migration Runbook

## Overview

SuperSonic's export/report artifacts are written through a pluggable `FileStorage` SPI.
Three backends: `local` (default), `oss` (Aliyun), `s3` (AWS or MinIO). Selected via
`s2.storage.type` in `application.yaml`.

## Deploying each backend

### Local (default, dev)

```yaml
s2:
  storage:
    type: local
    local:
      root-dir: /var/lib/supersonic/exports
```

Single-instance only (or shared NFS mount). No credentials needed.

### Aliyun OSS

```yaml
s2:
  storage:
    type: oss
    prefix: exports
    oss:
      endpoint: https://oss-cn-hangzhou.aliyuncs.com
      bucket: supersonic-prod
      access-key-id: ${S2_STORAGE_OSS_AK}
      access-key-secret: ${S2_STORAGE_OSS_SK}
```

Provision the bucket with a lifecycle rule deleting objects under `exports/` after 30 days.

### AWS S3 / MinIO

```yaml
s2:
  storage:
    type: s3
    prefix: exports
    s3:
      region: us-east-1
      bucket: supersonic-prod
      access-key: ${S2_STORAGE_S3_AK}
      secret-key: ${S2_STORAGE_S3_SK}
      # endpoint and path-style only needed for MinIO / non-AWS
      # endpoint: https://minio.example.com
      # path-style: true
```

Bucket IAM policy: grant `s3:GetObject`, `s3:PutObject`, `s3:DeleteObject` on `arn:aws:s3:::supersonic-prod/exports/*`.

## Migration from pre-SPI deployments

Prior to this change, `s2_export_task.file_location` contained absolute filesystem paths
like `/tmp/supersonic-export/export_42_20260417103000.xlsx`. After deploying:

- New rows use storage keys (e.g. `exports/7/20260417/42/export_42_20260417103000.xlsx`).
- Old rows: their `file_location` still resolves on the local FS of the original instance.
  These rows will fail to download from any other instance — accept the failure, let them
  expire via `ExportFileCleanupTask` (T+7 days by default).

No data migration script is required.

## Rollback

If the new backend misbehaves, switch back to local with zero downtime:

1. Set `S2_STORAGE_TYPE=local` in the environment.
2. Rolling-restart the fleet.
3. Any in-flight exports that were uploaded to the previous backend will fail on download
   (the key will not exist locally); users retry, new exports land on the local disk.

To fully roll back the code:

```bash
git revert <commit range for Tasks 1..9>
```

## Verification checklist (post-deploy)

- [ ] `GET /actuator/health` reports `UP`.
- [ ] Logs show the selected backend: `FileStorage: selecting <Impl> (s2.storage.type=<type>)`.
- [ ] Submit an export via `POST /api/v1/exportTasks`, wait for `SUCCESS` status.
- [ ] `s2_export_task.file_location` matches pattern `exports/<tenantId>/<yyyyMMdd>/<id>/<name>.{xlsx,csv}`.
- [ ] Download via `GET /api/v1/exportTasks/{id}:download` returns either a 200 with bytes (local) or a 302 with an OSS/S3 URL (cloud).
- [ ] After 7 days, `ExportFileCleanupTask` runs at 03:00 and objects disappear from the bucket.

## Metrics & alerts

Existing `TemplateReportMetrics` records export success/failure by format. No new metrics
are introduced in this migration — if a backend is flaky, failures will surface as elevated
error rate in `s2_template_report_export_seconds{outcome="error"}`.
```

- [ ] **Step 2: Commit**

```bash
git add docs/runbook/file-storage-migration.md
git commit -m "docs(runbook): file storage migration + rollback procedure"
```

---

## Final Verification

- [ ] **Step 1: Full compile**

Run: `mvn compile -pl launchers/standalone -am`
Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Full common-module test run**

Run: `mvn test -pl common`
Expected: all tests pass, including three `*FileStorageTest` and one `FileStorageAutoConfigurationTest`.

- [ ] **Step 3: Headless-server test run**

Run: `mvn test -pl headless/server`
Expected: `Tests run: N, Failures: 0, Errors: 0`.

- [ ] **Step 4: Confirm default behaviour is backwards compatible**

With no env vars set, start the standalone launcher and confirm log line:
`FileStorage: selecting LocalFileStorage (s2.storage.type=local)`.
Existing export flows must continue to work without any yaml edits.

---

## Spec Coverage Self-Review

| Requirement | Task |
|---|---|
| SPI interface with upload/download/delete/exists/presigned | Task 1 |
| FileStorageException | Task 1 |
| Tenant-namespaced paths | Task 1 (`StoragePath.forTenant`) + Task 8c (tenant re-check in controller) |
| Contract test suite (shared) | Task 2 |
| `LocalFileStorage` | Task 3 |
| `OssFileStorage` | Task 4 |
| `S3FileStorage` | Task 5 |
| Optional Maven deps via `<optional>true</optional>` | Task 4 step 1 |
| Testcontainers/MinIO | Task 4 + Task 5 |
| `@ConditionalOnProperty`-driven selection | Task 6 |
| `META-INF/spring.factories` registration | Task 6 step 2 |
| `s2.storage.*` YAML block | Task 6 step 3 |
| `DownloadServiceImpl` migration | Task 7 |
| Async export worker (`ExportTaskServiceImpl`) migration | Task 8a |
| Cleanup task migration | Task 8b |
| Frontend redirect handling | Task 9 |
| Runbook + rollback | Task 10 |
