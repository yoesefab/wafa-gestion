package com.wafabureau.gestion.util;

import java.util.UUID;

import org.slf4j.MDC;

public final class TraceIds {

    public static final String MDC_KEY = "traceId";
    public static final String RESPONSE_HEADER = "X-Trace-Id";

    private TraceIds() {
    }

    public static String currentOrCreate() {
        String traceId = MDC.get(MDC_KEY);
        return traceId == null || traceId.isBlank() ? UUID.randomUUID().toString() : traceId;
    }
}
