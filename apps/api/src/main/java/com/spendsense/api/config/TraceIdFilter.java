package com.spendsense.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class TraceIdFilter extends OncePerRequestFilter {
    static final String TRACE_HEADER = "X-Trace-Id";
    static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);
    private final SpendSenseProperties properties;

    public TraceIdFilter(SpendSenseProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        String traceId = request.getHeader(TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = traceId;
        }

        request.setAttribute("traceId", traceId);
        request.setAttribute("correlationId", correlationId);
        response.setHeader(TRACE_HEADER, traceId);
        response.setHeader(CORRELATION_HEADER, correlationId);
        MDC.put("traceId", traceId);
        MDC.put("correlationId", correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            long thresholdMs = slowRequestThresholdMs();
            if (elapsedMs >= thresholdMs) {
                log.warn(
                        "slow_api_request method={} path={} status={} durationMs={} traceId={} correlationId={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        elapsedMs,
                        traceId,
                        correlationId
                );
            }
            MDC.remove("traceId");
            MDC.remove("correlationId");
        }
    }

    private long slowRequestThresholdMs() {
        if (properties.performance() == null || properties.performance().slowRequestThresholdMs() == null) {
            return 750;
        }
        return Math.max(100, properties.performance().slowRequestThresholdMs());
    }
}
