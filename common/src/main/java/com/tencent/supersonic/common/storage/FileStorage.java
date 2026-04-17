package com.tencent.supersonic.common.storage;

import java.io.InputStream;
import java.time.Duration;

public interface FileStorage {

    void upload(String key, InputStream content, long sizeBytes);

    InputStream download(String key);

    void delete(String key);

    boolean exists(String key);

    String presignedUrl(String key, Duration ttl);

    String getStorageType();
}
