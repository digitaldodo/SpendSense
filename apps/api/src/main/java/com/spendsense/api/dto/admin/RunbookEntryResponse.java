package com.spendsense.api.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record RunbookEntryResponse(
        UUID id,
        String slug,
        String title,
        String severity,
        String category,
        String summary,
        String symptoms,
        String diagnosisSteps,
        String mitigationSteps,
        String escalationNotes,
        String relatedAlertKeys,
        Instant updatedAt
) {
}
