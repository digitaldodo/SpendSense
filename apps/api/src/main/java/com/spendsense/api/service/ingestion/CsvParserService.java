package com.spendsense.api.service.ingestion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CsvParserService {
    private static final long MAX_BYTES = 5 * 1024 * 1024;
    private static final int MAX_ROWS = 5_000;

    public ParsedCsv parse(MultipartFile file) {
        validateUpload(file);
        try {
            byte[] bytes = file.getBytes();
            String content = new String(bytes, StandardCharsets.UTF_8);
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1);
            }
            char delimiter = detectDelimiter(content);
            List<List<String>> lines = parseLines(content, delimiter);
            if (lines.isEmpty()) {
                throw new IllegalArgumentException("CSV file has no rows.");
            }
            List<String> headers = normalizeHeaders(lines.getFirst());
            if (headers.isEmpty()) {
                throw new IllegalArgumentException("CSV file must include a header row.");
            }
            List<CsvRow> rows = new ArrayList<>();
            for (int index = 1; index < lines.size(); index++) {
                if (rows.size() >= MAX_ROWS) {
                    throw new IllegalArgumentException("CSV imports currently support up to 5,000 rows per file.");
                }
                List<String> values = lines.get(index);
                if (values.stream().allMatch(String::isBlank)) {
                    continue;
                }
                Map<String, String> raw = new LinkedHashMap<>();
                for (int column = 0; column < headers.size(); column++) {
                    raw.put(headers.get(column), column < values.size() ? values.get(column).trim() : "");
                }
                rows.add(new CsvRow(index + 1, raw));
            }
            return new ParsedCsv(
                    safeFilename(file.getOriginalFilename()),
                    checksum(bytes),
                    signature(headers),
                    headers,
                    rows
            );
        } catch (IOException exception) {
            throw new IllegalArgumentException("CSV file could not be read.", exception);
        }
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a CSV file before importing.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("CSV file must be 5 MB or smaller for this import flow.");
        }
        String filename = safeFilename(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".csv")) {
            throw new IllegalArgumentException("Only .csv files are supported in this import flow.");
        }
    }

    private static char detectDelimiter(String content) {
        String firstLine = content.lines().findFirst().orElse("");
        int commas = count(firstLine, ',');
        int tabs = count(firstLine, '\t');
        int semicolons = count(firstLine, ';');
        if (tabs > commas && tabs > semicolons) {
            return '\t';
        }
        if (semicolons > commas) {
            return ';';
        }
        return ',';
    }

    private static int count(String value, char needle) {
        int count = 0;
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == needle) {
                count++;
            }
        }
        return count;
    }

    private static List<List<String>> parseLines(String content, char delimiter) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < content.length(); index++) {
            char current = content.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < content.length() && content.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == delimiter && !quoted) {
                row.add(field.toString());
                field.setLength(0);
            } else if ((current == '\n' || current == '\r') && !quoted) {
                if (current == '\r' && index + 1 < content.length() && content.charAt(index + 1) == '\n') {
                    index++;
                }
                row.add(field.toString());
                rows.add(row);
                row = new ArrayList<>();
                field.setLength(0);
            } else {
                field.append(current);
            }
        }
        row.add(field.toString());
        if (!row.stream().allMatch(String::isBlank)) {
            rows.add(row);
        }
        return rows;
    }

    private static List<String> normalizeHeaders(List<String> rawHeaders) {
        List<String> headers = new ArrayList<>();
        Map<String, Integer> seen = new LinkedHashMap<>();
        for (int index = 0; index < rawHeaders.size(); index++) {
            String header = rawHeaders.get(index).trim();
            if (header.isBlank()) {
                header = "Column " + (index + 1);
            }
            int count = seen.merge(header, 1, Integer::sum);
            headers.add(count == 1 ? header : header + " " + count);
        }
        return headers;
    }

    private static String safeFilename(String filename) {
        return filename == null || filename.isBlank() ? "transactions.csv" : filename.replaceAll("[\\\\/]+", "_");
    }

    private static String checksum(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest unavailable.", exception);
        }
    }

    private static String signature(List<String> headers) {
        return checksum(String.join("|", headers).toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    public record ParsedCsv(
            String filename,
            String checksum,
            String signature,
            List<String> headers,
            List<CsvRow> rows
    ) {
    }

    public record CsvRow(int rowNumber, Map<String, String> raw) {
    }
}
