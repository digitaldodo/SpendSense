package com.spendsense.api.dto.finance;

import com.spendsense.api.domain.finance.TransactionStatus;
import java.util.UUID;

public record TransactionUpdateRequest(
        UUID categoryId,
        TransactionStatus status,
        String reason
) {
}
