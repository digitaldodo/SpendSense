package com.spendsense.api.dto.finance;

public record DemoSeedResponse(
        int accountsCreated,
        int transactionsCreated,
        boolean alreadySeeded
) {
}
