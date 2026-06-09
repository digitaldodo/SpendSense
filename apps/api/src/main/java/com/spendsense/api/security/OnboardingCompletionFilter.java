package com.spendsense.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.common.ApiErrorResponse;
import com.spendsense.api.service.profile.ProfileService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class OnboardingCompletionFilter extends OncePerRequestFilter {
    private final ProfileService profileService;
    private final ObjectMapper objectMapper;

    public OnboardingCompletionFilter(ProfileService profileService, ObjectMapper objectMapper) {
        this.profileService = profileService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/api/v1/app/") || path.startsWith("/api/v1/dashboard/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();

        if (principal instanceof SupabasePrincipal supabasePrincipal
                && !profileService.isOnboardingComplete(supabasePrincipal.id())) {
            response.setStatus(HttpStatus.PRECONDITION_REQUIRED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiErrorResponse error = ApiErrorResponse.of(
                    "ONBOARDING_REQUIRED",
                    "Complete onboarding before using product APIs.",
                    Map.of("nextRoute", "/onboarding"),
                    (String) request.getAttribute("traceId")
            );
            objectMapper.writeValue(response.getWriter(), error);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
