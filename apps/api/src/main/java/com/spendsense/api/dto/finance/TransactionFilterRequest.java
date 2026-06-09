package com.spendsense.api.dto.finance;

import com.spendsense.api.domain.finance.TransactionDirection;
import com.spendsense.api.domain.finance.TransactionStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public record TransactionFilterRequest(
        UUID accountId,
        UUID categoryId,
        TransactionDirection direction,
        TransactionStatus status,
        String search,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant to,
        @Min(0)
        Integer page,
        @Min(1)
        @Max(100)
        Integer size,
        String sort
) {
}
