package com.spendsense.api.service.ingestion;

import com.spendsense.api.dto.finance.CsvColumnMappingRequest;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class CsvColumnMappingService {
    public CsvColumnMappingRequest mergeWithDetected(
            List<String> headers,
            CsvColumnMappingRequest requested
    ) {
        CsvColumnMappingRequest detected = detect(headers);
        if (requested == null) {
            return detected;
        }
        return new CsvColumnMappingRequest(
                first(requested.date(), detected.date()),
                first(requested.amount(), detected.amount()),
                first(requested.debitAmount(), detected.debitAmount()),
                first(requested.creditAmount(), detected.creditAmount()),
                first(requested.direction(), detected.direction()),
                first(requested.merchant(), detected.merchant()),
                first(requested.description(), detected.description()),
                first(requested.reference(), detected.reference()),
                first(requested.balance(), detected.balance()),
                first(requested.currency(), detected.currency())
        );
    }

    private CsvColumnMappingRequest detect(List<String> headers) {
        return new CsvColumnMappingRequest(
                find(headers, "date", "transaction date", "txn date", "value date", "posted date"),
                find(headers, "amount", "transaction amount", "withdrawal amount", "deposit amount"),
                find(headers, "debit", "withdrawal", "withdrawal amount", "paid", "debit amount", "dr"),
                find(headers, "credit", "deposit", "deposit amount", "received", "credit amount", "cr"),
                find(headers, "type", "direction", "dr/cr", "debit/credit"),
                find(headers, "merchant", "payee", "narration", "description", "remarks", "particulars"),
                find(headers, "description", "narration", "remarks", "particulars", "transaction details"),
                find(headers, "reference", "ref", "utr", "transaction id", "cheque/ref no", "chq/ref no"),
                find(headers, "balance", "closing balance", "available balance"),
                find(headers, "currency")
        );
    }

    private String find(List<String> headers, String... candidates) {
        for (String candidate : candidates) {
            for (String header : headers) {
                String normalizedHeader = normalize(header);
                String normalizedCandidate = normalize(candidate);
                if (normalizedHeader.equals(normalizedCandidate) || normalizedHeader.contains(normalizedCandidate)) {
                    return header;
                }
            }
        }
        return null;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static String first(String requested, String detected) {
        return requested == null || requested.isBlank() ? detected : requested;
    }
}
