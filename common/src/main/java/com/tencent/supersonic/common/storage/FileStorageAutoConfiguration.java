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
