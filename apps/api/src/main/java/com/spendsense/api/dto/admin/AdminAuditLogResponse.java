package com.spendsense.api.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditLogResponse(
        UUID id,
        UUID actorUserId,
        String actorEmail,
        String action,
        String targetType,
        UUID targetId,
        String reason,
        String traceId,
        Instant createdAt
) {
}
