package com.spendsense.api.dto.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AiFeedbackRequest(
        @Min(1) @Max(5) Integer rating,
        @Size(max = 48) String feedbackType,
        @Size(max = 1200) String comment
) {
}
