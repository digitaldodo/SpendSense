package com.spendsense.api.service.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.security.SupabasePrincipal;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AdminAuditService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AdminAuditService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void record(
            SupabasePrincipal principal,
            String action,
            String targetType,
            UUID targetId,
            String reason,
            Map<String, ?> metadata,
            String traceId
    ) {
        jdbcTemplate.update("""
                insert into admin_audit_logs (
                    id, actor_user_id, actor_email, action, target_type, target_id, reason, metadata_json, trace_id, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp)
                """,
                UUID.randomUUID(),
                principal.id(),
                principal.email(),
                action,
                targetType,
                targetId,
                trim(reason, 520),
                writeJson(metadata),
                traceId
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not write admin audit metadata.", exception);
        }
    }

    private String trim(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
