package com.spendsense.api.dto.finance;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        String colorToken,
        String iconName,
        boolean systemCategory
) {
}
