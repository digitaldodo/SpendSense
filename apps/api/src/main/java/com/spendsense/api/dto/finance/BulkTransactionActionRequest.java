package com.spendsense.api.dto.finance;

import com.spendsense.api.domain.finance.TransactionStatus;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record BulkTransactionActionRequest(
        @NotEmpty
        List<UUID> transactionIds,
        UUID categoryId,
        TransactionStatus status,
        String reason
) {
}
