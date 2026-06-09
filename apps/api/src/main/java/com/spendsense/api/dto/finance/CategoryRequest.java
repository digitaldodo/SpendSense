package com.spendsense.api.dto.finance;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank String name,
        String colorToken,
        String iconName,
        String reason
) {
}
