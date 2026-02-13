package com.tencent.supersonic.feishu.server.config;

import com.tencent.supersonic.feishu.api.config.FeishuProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@ConditionalOnProperty(name = "s2.feishu.enabled", havingValue = "true")
public class FeishuAsyncConfig {

    @Bean("feishuExecutor")
    public ThreadPoolTaskExecutor feishuExecutor(FeishuProperties properties) {
        FeishuProperties.AsyncConfig config = properties.getAsync();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.getCorePoolSize());
        executor.setMaxPoolSize(config.getMaxPoolSize());
        executor.setQueueCapacity(config.getQueueCapacity());
        executor.setThreadNamePrefix("feishu-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
