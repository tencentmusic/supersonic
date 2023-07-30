package com.tencent.supersonic.common.util;

import com.alibaba.ttl.TransmittableThreadLocal;

public class S2ThreadContext {

    private static final TransmittableThreadLocal<ThreadContext> context = new TransmittableThreadLocal<>();

    public ThreadContext get() {
        return context.get();
    }

    public void set(ThreadContext value) {
        context.set(value);
    }

    public void remove() {
        context.remove();
    }
}