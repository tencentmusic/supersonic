package com.tencent.supersonic.common.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.tencent.supersonic.common.util.ContextAwareThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class ThreadPoolConfig {

    @Bean("eventExecutor")
    public ThreadPoolExecutor getTaskEventExecutor() {
        return new ContextAwareThreadPoolExecutor(4, 8, 60 * 3, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1024),
                new ThreadFactoryBuilder().setNameFormat("supersonic-event-pool-").build(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Bean("commonExecutor")
    public ThreadPoolExecutor getCommonExecutor() {
        return new ContextAwareThreadPoolExecutor(8, 16, 60 * 3, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1024),
                new ThreadFactoryBuilder().setNameFormat("supersonic-common-pool-").build(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Bean("mapExecutor")
    public ThreadPoolExecutor getMapExecutor() {
        return new ContextAwareThreadPoolExecutor(8, 16, 60 * 3, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactoryBuilder().setNameFormat("supersonic-map-pool-").build(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Bean("chatExecutor")
    public ThreadPoolExecutor getChatExecutor() {
        return new ContextAwareThreadPoolExecutor(8, 16, 60 * 3, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1024),
                new ThreadFactoryBuilder().setNameFormat("supersonic-chat-pool-").build(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Bean("deployExecutor")
    public ThreadPoolExecutor getDeployExecutor() {
        return new ContextAwareThreadPoolExecutor(2, 4, 60 * 3, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(16),
                new ThreadFactoryBuilder().setNameFormat("supersonic-deploy-pool-").build(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Bean("exportExecutor")
    public ThreadPoolExecutor getExportExecutor() {
        return new ContextAwareThreadPoolExecutor(3, 3, 60 * 3, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(50),
                new ThreadFactoryBuilder().setNameFormat("supersonic-export-pool-").build(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
