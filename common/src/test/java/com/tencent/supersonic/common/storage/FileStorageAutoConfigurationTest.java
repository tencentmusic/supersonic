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
        runner.withPropertyValues("s2.storage.type=s3", "s2.storage.s3.region=us-east-1",
                "s2.storage.s3.bucket=test", "s2.storage.s3.access-key=ak",
                "s2.storage.s3.secret-key=sk", "s2.storage.s3.path-style=true").run(ctx -> {
                    assertThat(ctx.getBean(FileStorage.class)).isInstanceOf(S3FileStorage.class);
                });
    }
}
