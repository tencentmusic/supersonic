package com.tencent.supersonic.common.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class ThreadPoolConfig {

    @Bean("commonExecutor")
    public ThreadPoolExecutor getCommonExecutor() {
        return new ThreadPoolExecutor(8, 16, 60 * 3, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(1024),
                new ThreadFactoryBuilder().setNameFormat("supersonic-common-pool-").build(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Bean("mapExecutor")
    public ThreadPoolExecutor getMapExecutor() {
        return new ThreadPoolExecutor(8, 16, 60 * 3, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(8192),
                new ThreadFactoryBuilder().setNameFormat("supersonic-map-pool-").build(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
