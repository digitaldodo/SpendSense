package com.spendsense.api.dto.auth;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AuthenticatedUserResponse(
        UUID id,
        String email,
        Set<String> roles,
        Instant authenticatedAt
) {
}
