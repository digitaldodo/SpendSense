package com.spendsense.api.service.ops;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.config.SpendSenseProperties;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class OperationalTraceService {
    private static final Logger log = LoggerFactory.getLogger(OperationalTraceService.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final SpendSenseProperties properties;

    public OperationalTraceService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            SpendSenseProperties properties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public void record(
            String eventType,
            String severity,
            String source,
            String sourceId,
            String traceId,
            String message,
            Map<String, ?> metadata
    ) {
        try {
            SpendSenseProperties.Operations operations = properties.operations();
            jdbcTemplate.update("""
                    insert into operational_trace_events (
                        id, event_type, severity, environment, release_version, release_commit, source, source_id,
                        trace_id, message, metadata_json, observed_at, created_at
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                    """,
                    UUID.randomUUID(),
                    trim(eventType, 96),
                    trim(severity, 24),
                    trim(operations.environment(), 40),
                    trim(operations.releaseVersion(), 120),
                    trim(operations.releaseCommit(), 120),
                    trim(source, 120),
                    trim(sourceId, 180),
                    trim(traceId, 120),
                    trim(message, 720),
                    writeJson(metadata)
            );
        } catch (DataAccessException exception) {
            log.warn("operational_trace_record_failed eventType={} source={} error={}", eventType, source, exception.getMessage());
        }
    }

    private String writeJson(Map<String, ?> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private String trim(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
