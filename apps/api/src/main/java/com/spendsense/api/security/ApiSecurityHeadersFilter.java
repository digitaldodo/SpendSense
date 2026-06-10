package com.spendsense.api.security;

import com.spendsense.api.config.SpendSenseProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ApiSecurityHeadersFilter extends OncePerRequestFilter {
    private static final String DEFAULT_CSP = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";
    private static final String DEFAULT_PERMISSIONS_POLICY = "camera=(), microphone=(), geolocation=(), payment=()";
    private final SpendSenseProperties properties;

    public ApiSecurityHeadersFilter(SpendSenseProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        writeSecurityHeaders(response);
        if (containsControlChars(request.getRequestURI()) || containsControlChars(request.getQueryString())) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Request path contains invalid characters.");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeSecurityHeaders(HttpServletResponse response) {
        response.setHeader("Content-Security-Policy", securityHeaders().contentSecurityPolicy());
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", securityHeaders().permissionsPolicy());
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        response.setHeader("Cross-Origin-Resource-Policy", "same-site");
    }

    private SpendSenseProperties.Headers securityHeaders() {
        SpendSenseProperties.Headers configured = properties.security() == null ? null : properties.security().headers();
        String csp = configured == null ? null : configured.contentSecurityPolicy();
        String permissionsPolicy = configured == null ? null : configured.permissionsPolicy();
        return new SpendSenseProperties.Headers(
                StringUtils.hasText(csp) ? csp : DEFAULT_CSP,
                StringUtils.hasText(permissionsPolicy) ? permissionsPolicy : DEFAULT_PERMISSIONS_POLICY
        );
    }

    private boolean containsControlChars(String value) {
        if (value == null) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isISOControl(current) && current != '\t') {
                return true;
            }
        }
        return false;
    }
}
