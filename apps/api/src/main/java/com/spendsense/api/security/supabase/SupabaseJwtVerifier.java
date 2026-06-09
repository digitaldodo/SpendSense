package com.spendsense.api.security.supabase;

import com.spendsense.api.config.SpendSenseProperties;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.security.UserRole;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SupabaseJwtVerifier {
    private final SpendSenseProperties.Supabase properties;
    private final JwtDecoder jwtDecoder;

    public SupabaseJwtVerifier(SpendSenseProperties properties) {
        this.properties = properties.security().supabase();
        this.jwtDecoder = createDecoder(this.properties);
    }

    public SupabasePrincipal verify(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BadCredentialsException("Missing bearer token.");
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);
            validateIssuer(jwt);
            validateAudience(jwt);
            UUID userId = UUID.fromString(jwt.getSubject());
            String email = jwt.getClaimAsString("email");
            return new SupabasePrincipal(userId, email, extractRoles(jwt), jwt.getClaims());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BadCredentialsException("Supabase token validation failed.", exception);
        }
    }

    private JwtDecoder createDecoder(SpendSenseProperties.Supabase supabase) {
        if (StringUtils.hasText(supabase.jwksUri())) {
            return NimbusJwtDecoder.withJwkSetUri(supabase.jwksUri()).build();
        }

        if (StringUtils.hasText(supabase.jwtSecret())) {
            SecretKeySpec secretKey = new SecretKeySpec(
                    supabase.jwtSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            return NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
        }

        return token -> {
            throw new BadCredentialsException("Supabase JWT verifier is not configured.");
        };
    }

    private void validateIssuer(Jwt jwt) {
        if (jwt.getIssuer() == null || !properties.issuer().equals(jwt.getIssuer().toString())) {
            throw new BadCredentialsException("Supabase token issuer is not allowed.");
        }
    }

    private void validateAudience(Jwt jwt) {
        String audience = properties.audience();
        if (StringUtils.hasText(audience) && !jwt.getAudience().contains(audience)) {
            throw new BadCredentialsException("Supabase token audience is not allowed.");
        }
    }

    private Set<UserRole> extractRoles(Jwt jwt) {
        Set<UserRole> roles = new LinkedHashSet<>();
        roles.add(UserRole.USER);
        addRole(roles, jwt.getClaimAsString("role"));
        addRolesFromMetadata(roles, jwt.getClaim("app_metadata"));
        return roles;
    }

    private void addRolesFromMetadata(Set<UserRole> roles, Object metadata) {
        if (!(metadata instanceof Map<?, ?> appMetadata)) {
            return;
        }

        addRole(roles, appMetadata.get("role"));
        Object roleClaims = appMetadata.get("roles");
        if (roleClaims instanceof Collection<?> collection) {
            collection.forEach(role -> addRole(roles, role));
        }
    }

    private void addRole(Set<UserRole> roles, Object value) {
        if (value == null) {
            return;
        }

        String normalized = value.toString().replace("ROLE_", "").toUpperCase();
        try {
            roles.add(UserRole.valueOf(normalized));
        } catch (IllegalArgumentException ignored) {
            roles.add(UserRole.USER);
        }
    }
}
