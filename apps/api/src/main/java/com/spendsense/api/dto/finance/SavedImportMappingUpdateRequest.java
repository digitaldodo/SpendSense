package com.spendsense.api.dto.finance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SavedImportMappingUpdateRequest(
        @NotBlank
        @Size(max = 160)
        String name
) {
}
