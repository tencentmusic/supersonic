package com.tencent.supersonic.common.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ThreadPoolExecutor that automatically propagates MDC and TenantContext from the submitting thread
 * to the worker thread via {@link ThreadMdcUtil}. Implements {@link DisposableBean} to wait for
 * in-flight tasks to complete before shutdown.
 */
@Slf4j
public class ContextAwareThreadPoolExecutor extends ThreadPoolExecutor implements DisposableBean {

    private static final int AWAIT_TERMINATION_SECONDS = 30;

    public ContextAwareThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
            TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
            RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory,
                handler);
    }

    @Override
    public void execute(Runnable command) {
        super.execute(ThreadMdcUtil.wrap(command, MDC.getCopyOfContextMap()));
    }

    @Override
    public void destroy() {
        String poolName = getThreadFactory().toString();
        log.info("Shutting down executor [{}], active={}, queued={}", poolName, getActiveCount(),
                getQueue().size());
        shutdown();
        try {
            if (!awaitTermination(AWAIT_TERMINATION_SECONDS, TimeUnit.SECONDS)) {
                log.warn(
                        "Executor [{}] did not terminate in {}s, forcing shutdown. "
                                + "Dropped {} queued tasks",
                        poolName, AWAIT_TERMINATION_SECONDS, shutdownNow().size());
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while awaiting executor [{}] termination, forcing shutdown",
                    poolName);
            shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
