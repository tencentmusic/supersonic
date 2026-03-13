package com.tencent.supersonic.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Ensures custom ThreadPoolExecutor beans (from {@link ThreadPoolConfig}) are shut down gracefully
 * when the application context closes. Raw {@link java.util.concurrent.ThreadPoolExecutor} beans
 * are not managed by Spring's lifecycle, so this component explicitly shuts them down.
 */
@Component
@Slf4j
public class ExecutorGracefulShutdown implements DisposableBean {

    private static final int AWAIT_SECONDS = 30;

    private final List<ExecutorService> executors;

    public ExecutorGracefulShutdown(@Qualifier("eventExecutor") ExecutorService eventExecutor,
            @Qualifier("commonExecutor") ExecutorService commonExecutor,
            @Qualifier("mapExecutor") ExecutorService mapExecutor,
            @Qualifier("chatExecutor") ExecutorService chatExecutor,
            @Qualifier("deployExecutor") ExecutorService deployExecutor,
            @Qualifier("exportExecutor") ExecutorService exportExecutor) {
        this.executors = Arrays.asList(eventExecutor, commonExecutor, mapExecutor, chatExecutor,
                deployExecutor, exportExecutor);
    }

    @Override
    public void destroy() {
        for (ExecutorService executor : executors) {
            try {
                executor.shutdown();
                if (!executor.awaitTermination(AWAIT_SECONDS, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        log.warn("Executor did not terminate in time: {}", executor);
                    }
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
                log.warn("Interrupted while shutting down executor: {}", executor);
            }
        }
    }
}
