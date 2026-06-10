package com.spendsense.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.common.ApiErrorResponse;
import com.spendsense.api.config.SpendSenseProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class RateLimitingFilter extends OncePerRequestFilter {
    private final SpendSenseProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(SpendSenseProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        SpendSenseProperties.RateLimit rateLimit = rateLimit();
        return !Boolean.TRUE.equals(rateLimit.enabled())
                || request.getRequestURI().startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String key = clientKey(request);
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(capacity()));
        if (!bucket.tryConsume(requestsPerMinute(), capacity())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute()));
            response.setHeader("X-RateLimit-Remaining", "0");
            objectMapper.writeValue(response.getWriter(), ApiErrorResponse.of(
                    "RATE_LIMITED",
                    "Too many requests. Please retry shortly.",
                    Map.of(),
                    (String) request.getAttribute("traceId")
            ));
            return;
        }
        response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, bucket.remaining())));
        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ip = forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr()
                : forwardedFor.split(",")[0].trim();
        Object principal = request.getUserPrincipal();
        return principal == null ? ip : principal.toString();
    }

    private int requestsPerMinute() {
        Integer configured = rateLimit().requestsPerMinute();
        return configured == null ? 120 : Math.max(1, configured);
    }

    private int capacity() {
        Integer configured = rateLimit().burstCapacity();
        return configured == null ? requestsPerMinute() : Math.max(1, configured);
    }

    private SpendSenseProperties.RateLimit rateLimit() {
        if (properties.security() == null || properties.security().rateLimit() == null) {
            return new SpendSenseProperties.RateLimit(true, 120, 120);
        }
        return properties.security().rateLimit();
    }

    private static final class Bucket {
        private int tokens;
        private long windowStartedAtEpochSecond;

        private Bucket(int capacity) {
            this.tokens = capacity;
            this.windowStartedAtEpochSecond = Instant.now().getEpochSecond();
        }

        synchronized boolean tryConsume(int refillAmount, int capacity) {
            long now = Instant.now().getEpochSecond();
            if (now - windowStartedAtEpochSecond >= 60) {
                tokens = Math.min(capacity, refillAmount);
                windowStartedAtEpochSecond = now;
            }
            if (tokens <= 0) {
                return false;
            }
            tokens--;
            return true;
        }

        synchronized int remaining() {
            return tokens;
        }
    }
}
