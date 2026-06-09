package com.spendsense.api.security;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record SupabasePrincipal(
        UUID id,
        String email,
        Set<UserRole> roles,
        Map<String, Object> claims
) {
}
