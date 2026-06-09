package com.spendsense.api.dto.finance;

public record BulkTransactionActionResponse(
        int requested,
        int updated
) {
}
