package com.tencent.supersonic.common.util;

import com.tencent.supersonic.common.context.TenantContext;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class ThreadMdcUtil {
    public static void setTraceIdIfAbsent() {
        if (MDC.get(TraceIdUtil.TRACE_ID) == null) {
            MDC.put(TraceIdUtil.TRACE_ID, TraceIdUtil.generateTraceId());
        }
    }

    public static <T> Callable<T> wrap(final Callable<T> callable,
            final Map<String, String> context) {
        final Long tenantId = TenantContext.getTenantId();
        return () -> {
            if (context == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(context);
            }
            setTraceIdIfAbsent();
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
            }
            try {
                return callable.call();
            } finally {
                MDC.clear();
                TenantContext.clear();
            }
        };
    }

    public static Runnable wrap(final Runnable runnable, final Map<String, String> context) {
        final Long tenantId = TenantContext.getTenantId();
        return () -> {
            if (context == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(context);
            }
            setTraceIdIfAbsent();
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
            }
            try {
                runnable.run();
            } finally {
                MDC.clear();
                TenantContext.clear();
            }
        };
    }

    public static <T> Supplier<T> wrapSupplier(final Supplier<T> supplier,
            final Map<String, String> context) {
        final Long tenantId = TenantContext.getTenantId();
        return () -> {
            if (context == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(context);
            }
            setTraceIdIfAbsent();
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
            }
            try {
                return supplier.get();
            } finally {
                MDC.clear();
                TenantContext.clear();
            }
        };
    }
}
