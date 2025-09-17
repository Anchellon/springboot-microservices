package com.example.department.util;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class TraceIdUtil {

    private static final String TRACE_ID_MDC_KEY = "traceId";

    public static String getCurrentTraceId() {
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        if (traceId == null) {
            traceId = generateTraceId();
            MDC.put(TRACE_ID_MDC_KEY, traceId);
        }
        return traceId;
    }

    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID_MDC_KEY, traceId);
    }

    public static void clearTraceId() {
        MDC.remove(TRACE_ID_MDC_KEY);
    }
}