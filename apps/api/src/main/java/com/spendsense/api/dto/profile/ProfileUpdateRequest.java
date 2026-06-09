package com.spendsense.api.dto.profile;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @Size(max = 160, message = "Display name must be 160 characters or fewer.")
        String displayName
) {
}
