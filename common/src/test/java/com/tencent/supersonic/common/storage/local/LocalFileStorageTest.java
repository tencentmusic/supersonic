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
