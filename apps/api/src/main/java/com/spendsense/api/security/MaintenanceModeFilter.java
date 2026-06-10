package com.spendsense.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.common.ApiErrorResponse;
import com.spendsense.api.config.SpendSenseProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MaintenanceModeFilter extends OncePerRequestFilter {
    private final SpendSenseProperties properties;
    private final ObjectMapper objectMapper;

    public MaintenanceModeFilter(SpendSenseProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!maintenanceModeEnabled() || isAllowedDuringMaintenance(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String traceId = (String) request.getAttribute("traceId");
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "300");
        objectMapper.writeValue(response.getWriter(), ApiErrorResponse.of(
                "MAINTENANCE_MODE",
                "SpendSense is temporarily read-only while planned maintenance completes.",
                Map.of(),
                traceId
        ));
    }

    private boolean maintenanceModeEnabled() {
        return Boolean.TRUE.equals(properties.operations().maintenanceMode());
    }

    private boolean isAllowedDuringMaintenance(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return HttpMethod.GET.matches(method)
                || HttpMethod.HEAD.matches(method)
                || HttpMethod.OPTIONS.matches(method)
                || path.startsWith("/api/v1/health")
                || path.startsWith("/actuator/health");
    }
}
