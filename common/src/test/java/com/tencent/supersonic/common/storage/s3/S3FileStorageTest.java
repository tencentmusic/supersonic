package com.tencent.supersonic.common.storage.s3;

import com.tencent.supersonic.common.storage.AbstractFileStorageContractTest;
import com.tencent.supersonic.common.storage.FileStorage;
import com.tencent.supersonic.common.storage.StorageProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.ProxyConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

@Testcontainers
class S3FileStorageTest extends AbstractFileStorageContractTest {

    @Container
    static MinIOContainer MINIO = new MinIOContainer("minio/minio:RELEASE.2024-09-13T20-26-02Z")
            .withUserName("minioadmin").withPassword("minioadmin");

    private static final String BUCKET = "supersonic-test-s3";

    /**
     * Build an S3Client that bypasses any HTTP_PROXY env var (which would route test traffic
     * through an external proxy instead of reaching the local MinIO container).
     */
    private static S3Client adminClient() {
        return S3Client.builder().endpointOverride(URI.create(MINIO.getS3URL()))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(MINIO.getUserName(), MINIO.getPassword())))
                .serviceConfiguration(
                        S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .httpClientBuilder(UrlConnectionHttpClient.builder().proxyConfiguration(
                        ProxyConfiguration.builder().useSystemPropertyValues(false).build()))
                .build();
    }

    @BeforeAll
    static void createBucket() throws InterruptedException {
        // MinIO passes the HTTP health check before the S3 API is fully ready.
        // Retry with backoff to handle the transient 503 on startup.
        Exception last = null;
        for (int attempt = 0; attempt < 10; attempt++) {
            try (S3Client s3 = adminClient()) {
                try {
                    s3.headBucket(b -> b.bucket(BUCKET));
                } catch (Exception e) {
                    s3.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
                }
                return; // success
            } catch (Exception e) {
                last = e;
                Thread.sleep(500);
            }
        }
        throw new IllegalStateException("MinIO bucket setup failed after 10 attempts", last);
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
        props.getS3().setNoProxy(true); // bypass HTTP_PROXY for local MinIO
        return new S3FileStorage(props);
    }

    @Override
    protected boolean supportsPresign() {
        return true;
    }
}
