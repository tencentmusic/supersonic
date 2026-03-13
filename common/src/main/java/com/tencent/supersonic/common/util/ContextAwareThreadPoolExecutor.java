package com.tencent.supersonic.common.util;

import org.slf4j.MDC;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ThreadPoolExecutor that automatically propagates MDC and TenantContext from the submitting thread
 * to the worker thread via {@link ThreadMdcUtil}.
 */
public class ContextAwareThreadPoolExecutor extends ThreadPoolExecutor {

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
}
