package com.spendsense.api.security;

import com.spendsense.api.security.supabase.SupabaseJwtVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final SupabaseJwtVerifier supabaseJwtVerifier;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
            SupabaseJwtVerifier supabaseJwtVerifier,
            AuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.supabaseJwtVerifier = supabaseJwtVerifier;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = resolveBearerToken(request);
            if (StringUtils.hasText(token)) {
                SupabasePrincipal principal = supabaseJwtVerifier.verify(token);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        token,
                        principal.roles().stream()
                                .map(UserRole::authority)
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toUnmodifiableSet())
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        } catch (BadCredentialsException | AuthenticationServiceException exception) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, exception);
        }
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
}
