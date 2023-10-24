package com.tencent.supersonic.common.util;

import org.slf4j.MDC;

import java.util.UUID;

public class TraceIdUtil {
    public static final String TRACE_ID = "traceId";

    public static final String PREFIX = "supersonic";

    public static String getTraceId() {
        String traceId = (String) MDC.get(TRACE_ID);
        return traceId == null ? "" : traceId;
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    public static void remove() {
        MDC.remove(TRACE_ID);

    }

    public static void clear() {
        MDC.clear();
    }

    public static String generateTraceId() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return PREFIX + "_" + uuid;
    }
}
