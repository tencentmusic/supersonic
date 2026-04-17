package com.tencent.supersonic.common.storage.oss;

import com.tencent.supersonic.common.storage.AbstractFileStorageContractTest;
import com.tencent.supersonic.common.storage.FileStorage;
import com.tencent.supersonic.common.storage.StorageProperties;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Contract tests for OssFileStorage against a real Aliyun OSS endpoint.
 *
 * <p>
 * MinIO cannot be used here because it requires AWS4-HMAC-SHA256 signing, but the Aliyun OSS SDK
 * uses its own OSS4-HMAC-SHA256 signing scheme. Run this test by setting the following env
 * variables:
 *
 * <pre>
 *   OSS_ENDPOINT      e.g. https://oss-cn-hangzhou.aliyuncs.com
 *   OSS_BUCKET        e.g. my-test-bucket
 *   OSS_ACCESS_KEY    Alibaba Cloud Access Key ID
 *   OSS_SECRET_KEY    Alibaba Cloud Access Key Secret
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "OSS_ENDPOINT", matches = ".+")
class OssFileStorageTest extends AbstractFileStorageContractTest {

    @Override
    protected FileStorage createStorage() {
        StorageProperties props = new StorageProperties();
        props.setType("oss");
        props.getOss().setEndpoint(System.getenv("OSS_ENDPOINT"));
        props.getOss().setBucket(System.getenv("OSS_BUCKET"));
        props.getOss().setAccessKeyId(System.getenv("OSS_ACCESS_KEY"));
        props.getOss().setAccessKeySecret(System.getenv("OSS_SECRET_KEY"));
        return new OssFileStorage(props);
    }

    @Override
    protected boolean supportsPresign() {
        return true;
    }
}
