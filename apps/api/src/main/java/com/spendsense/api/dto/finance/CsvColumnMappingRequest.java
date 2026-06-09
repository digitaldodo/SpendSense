package com.spendsense.api.dto.finance;

public record CsvColumnMappingRequest(
        String date,
        String amount,
        String debitAmount,
        String creditAmount,
        String direction,
        String merchant,
        String description,
        String reference,
        String balance,
        String currency
) {
}
