package com.spendsense.api.dto.engagement;

import jakarta.validation.constraints.NotBlank;

public record ScheduledReportRequest(
        @NotBlank String reportType,
        @NotBlank String format,
        @NotBlank String cadence,
        String timezone,
        String deliveryChannel,
        Boolean active
) {
}
