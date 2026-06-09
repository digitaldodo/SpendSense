package com.spendsense.api.service.ingestion;

import com.spendsense.api.domain.finance.TransactionDirection;
import com.spendsense.api.dto.finance.CsvColumnMappingRequest;
import com.spendsense.api.service.ingestion.CsvParserService.CsvRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransactionNormalizationService {
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            formatter("dd/MM/uuuu"),
            formatter("dd-MM-uuuu"),
            formatter("dd.MM.uuuu"),
            formatter("MM/dd/uuuu"),
            formatter("uuuu/MM/dd"),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd MMM uuuu").toFormatter(Locale.ENGLISH)
    );
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            formatter("dd/MM/uuuu HH:mm:ss"),
            formatter("dd/MM/uuuu HH:mm"),
            formatter("dd-MM-uuuu HH:mm:ss"),
            formatter("dd-MM-uuuu HH:mm")
    );

    public NormalizedTransaction normalize(CsvRow row, CsvColumnMappingRequest mapping, UUID userProfileId, UUID accountId) {
        String dateValue = value(row, mapping.date());
        Instant occurredAt = parseDate(dateValue);
        AmountDirection amountDirection = parseAmountAndDirection(row, mapping);
        String description = firstNonBlank(value(row, mapping.description()), value(row, mapping.merchant()));
        String merchantName = firstNonBlank(value(row, mapping.merchant()), description, "Unknown merchant");
        String merchantNormalized = normalizeMerchant(merchantName);
        String reference = blankToNull(value(row, mapping.reference()));
        String currency = firstNonBlank(value(row, mapping.currency()), "INR").toUpperCase(Locale.ROOT);
        if (currency.length() > 3) {
            currency = "INR";
        }
        String fingerprint = fingerprint(String.join(
                "|",
                userProfileId.toString(),
                occurredAt.toString(),
                amountDirection.amount().toPlainString(),
                amountDirection.direction().name(),
                merchantNormalized,
                reference == null ? "" : reference.toLowerCase(Locale.ROOT)
        ));
        return new NormalizedTransaction(
                row.rowNumber(),
                occurredAt,
                amountDirection.amount(),
                amountDirection.direction(),
                merchantName,
                merchantNormalized,
                blankToNull(description),
                reference,
                currency,
                fingerprint
        );
    }

    public String normalizeMerchant(String value) {
        String normalized = value == null ? "" : value
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\b(upi|imps|neft|rtgs|pos|to|from|paytm|phonepe|gpay|google pay)\\b", " ")
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
        return normalized.isBlank() ? "unknown merchant" : normalized;
    }

    private AmountDirection parseAmountAndDirection(CsvRow row, CsvColumnMappingRequest mapping) {
        BigDecimal debit = parseOptionalAmount(value(row, mapping.debitAmount()));
        BigDecimal credit = parseOptionalAmount(value(row, mapping.creditAmount()));
        if (credit != null && credit.signum() != 0) {
            return new AmountDirection(credit.abs(), TransactionDirection.CREDIT);
        }
        if (debit != null && debit.signum() != 0) {
            return new AmountDirection(debit.abs(), TransactionDirection.DEBIT);
        }

        BigDecimal amount = parseRequiredAmount(value(row, mapping.amount()));
        TransactionDirection direction = inferDirection(value(row, mapping.direction()), amount);
        return new AmountDirection(amount.abs(), direction);
    }

    private TransactionDirection inferDirection(String directionValue, BigDecimal amount) {
        String normalized = directionValue == null ? "" : directionValue.toLowerCase(Locale.ROOT);
        if (normalized.contains("cr") || normalized.contains("credit") || normalized.contains("deposit")
                || normalized.contains("received")) {
            return TransactionDirection.CREDIT;
        }
        if (normalized.contains("dr") || normalized.contains("debit") || normalized.contains("withdraw")
                || normalized.contains("paid")) {
            return TransactionDirection.DEBIT;
        }
        return amount.signum() < 0 ? TransactionDirection.DEBIT : TransactionDirection.CREDIT;
    }

    private Instant parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Transaction date is missing.");
        }
        String trimmed = value.trim();
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(trimmed, formatter).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
                // Try the next known bank export format.
            }
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(trimmed, formatter).atStartOfDay().toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
                // Try the next known bank export format.
            }
        }
        throw new IllegalArgumentException("Transaction date could not be understood.");
    }

    private BigDecimal parseRequiredAmount(String value) {
        BigDecimal amount = parseOptionalAmount(value);
        if (amount == null || amount.signum() == 0) {
            throw new IllegalArgumentException("Transaction amount is missing.");
        }
        return amount;
    }

    private BigDecimal parseOptionalAmount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        boolean parenthesized = trimmed.startsWith("(") && trimmed.endsWith(")");
        String cleaned = trimmed
                .replace("₹", "")
                .replace("INR", "")
                .replace("Rs.", "")
                .replace("Rs", "")
                .replace(",", "")
                .replace("(", "")
                .replace(")", "")
                .replaceAll("[^0-9.\\-]", "");
        if (cleaned.isBlank() || cleaned.equals("-")) {
            return null;
        }
        BigDecimal amount = new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
        return parenthesized ? amount.abs().negate() : amount;
    }

    private String value(CsvRow row, String header) {
        return header == null || header.isBlank() ? null : row.raw().get(header);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static DateTimeFormatter formatter(String pattern) {
        return DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.SMART);
    }

    private static String fingerprint(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest unavailable.", exception);
        }
    }

    private record AmountDirection(BigDecimal amount, TransactionDirection direction) {
    }

    public record NormalizedTransaction(
            int rowNumber,
            Instant occurredAt,
            BigDecimal amount,
            TransactionDirection direction,
            String merchantName,
            String merchantNormalized,
            String description,
            String reference,
            String currency,
            String dedupeFingerprint
    ) {
    }
}
