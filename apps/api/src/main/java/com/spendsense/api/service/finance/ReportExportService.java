package com.spendsense.api.service.finance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.dto.finance.CategorySpendResponse;
import com.spendsense.api.dto.finance.FinancialInsightsResponse;
import com.spendsense.api.dto.finance.GeneratedReportResponse;
import com.spendsense.api.dto.finance.MonthlyComparisonResponse;
import com.spendsense.api.dto.finance.RecurringPatternResponse;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.user.UserProfileSyncService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportExportService {
    private final FinancialInsightsService financialInsightsService;
    private final UserProfileSyncService userProfileSyncService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ReportExportService(
            FinancialInsightsService financialInsightsService,
            UserProfileSyncService userProfileSyncService,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.financialInsightsService = financialInsightsService;
        this.userProfileSyncService = userProfileSyncService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public GeneratedReportResponse monthlyReport(SupabasePrincipal principal, YearMonth month) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        YearMonth reportMonth = month == null ? YearMonth.from(LocalDate.now(clock)) : month;
        LocalDate start = reportMonth.atDay(1);
        LocalDate end = reportMonth.atEndOfMonth();
        FinancialInsightsResponse insights = financialInsightsService.buildInsights(userProfileId, start.minusMonths(5), end, false);
        List<CategorySpendResponse> categories = financialInsightsService.categoryBreakdown(userProfileId, start, end);
        UUID reportId = recordReport(userProfileId, "MONTHLY_SUMMARY", "JSON", start, end, "spendsense-%s-report.json".formatted(reportMonth));
        return new GeneratedReportResponse(reportId, "MONTHLY_SUMMARY", "JSON", Instant.now(clock), insights, categories);
    }

    @Transactional
    public ExportFile csvExport(SupabasePrincipal principal, LocalDate from, LocalDate to, String reportType) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        LocalDate start = from == null ? LocalDate.now(clock).withDayOfMonth(1) : from;
        LocalDate end = to == null ? LocalDate.now(clock).withDayOfMonth(1).plusMonths(1).minusDays(1) : to;
        FinancialInsightsResponse insights = financialInsightsService.buildInsights(userProfileId, start, end, false);
        String normalizedType = reportType == null || reportType.isBlank() ? "monthly-summary" : reportType;
        String filename = "spendsense-%s-%s-to-%s.csv".formatted(normalizedType, start, end);
        recordReport(userProfileId, normalizedType.toUpperCase().replace("-", "_"), "CSV", start, end, filename);
        return new ExportFile(filename, "text/csv", csv(insights, normalizedType).getBytes(StandardCharsets.UTF_8));
    }

    @Transactional
    public ExportFile pdfExport(SupabasePrincipal principal, YearMonth month) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        YearMonth reportMonth = month == null ? YearMonth.from(LocalDate.now(clock)) : month;
        LocalDate start = reportMonth.atDay(1);
        LocalDate end = reportMonth.atEndOfMonth();
        FinancialInsightsResponse insights = financialInsightsService.buildInsights(userProfileId, start.minusMonths(5), end, false);
        String filename = "spendsense-%s-summary.pdf".formatted(reportMonth);
        recordReport(userProfileId, "MONTHLY_SUMMARY", "PDF", start, end, filename);
        return new ExportFile(filename, "application/pdf", simplePdf(linesForPdf(insights)));
    }

    private String csv(FinancialInsightsResponse insights, String reportType) {
        StringBuilder builder = new StringBuilder();
        if (reportType.equals("category-report")) {
            builder.append("category,total_spend,average_monthly_spend,latest_month_spend,trend_percent\n");
            insights.categoryDeepDives().forEach(category -> builder.append(csvRow(List.of(
                    category.categoryName(),
                    category.totalSpend(),
                    category.averageMonthlySpend(),
                    category.latestMonthSpend(),
                    category.trendPercent()
            ))));
            return builder.toString();
        }
        builder.append("month,income,expense,net_cashflow,savings_rate,expense_change_percent\n");
        for (MonthlyComparisonResponse month : insights.monthlyComparisons()) {
            builder.append(csvRow(List.of(
                    month.periodStart().toString(),
                    month.income(),
                    month.expense(),
                    month.netCashflow(),
                    month.savingsRate(),
                    month.expenseChangePercent()
            )));
        }
        builder.append("\nsubscriptions,merchant,amount,cadence,next_expected,confidence\n");
        for (RecurringPatternResponse subscription : insights.subscriptions()) {
            builder.append(csvRow(List.of(
                    "subscription",
                    subscription.merchantName(),
                    subscription.amount(),
                    subscription.cadence(),
                    subscription.nextExpectedOn(),
                    subscription.confidence()
            )));
        }
        return builder.toString();
    }

    private String csvRow(List<?> values) {
        return values.stream()
                .map(value -> value == null ? "" : value.toString())
                .map(value -> "\"" + value.replace("\"", "\"\"") + "\"")
                .reduce((left, right) -> left + "," + right)
                .orElse("")
                + "\n";
    }

    private List<String> linesForPdf(FinancialInsightsResponse insights) {
        List<String> lines = new ArrayList<>();
        lines.add("SpendSense Monthly Financial Summary");
        lines.add("Period: " + insights.periodLabel());
        lines.add("Income: " + money(insights.summary().income()));
        lines.add("Expenses: " + money(insights.summary().expense()));
        lines.add("Net cashflow: " + money(insights.summary().netCashflow()));
        lines.add("Savings rate: " + insights.summary().savingsRate() + "%");
        lines.add("Recurring spend: " + money(insights.summary().recurringSpend()));
        lines.add("Spending spikes: " + insights.anomalies().size());
        lines.add("Income stability: " + insights.incomeStability().state());
        lines.add("Top deterministic insights:");
        insights.insights().stream().limit(5).forEach(insight -> lines.add("- " + insight.title() + ": " + insight.body()));
        return lines;
    }

    private String money(BigDecimal value) {
        return "INR " + value;
    }

    private byte[] simplePdf(List<String> lines) {
        StringBuilder text = new StringBuilder("BT /F1 12 Tf 54 770 Td 16 TL ");
        for (String line : lines) {
            text.append("(").append(escapePdf(line)).append(") Tj T* ");
        }
        text.append("ET");
        byte[] stream = text.toString().getBytes(StandardCharsets.UTF_8);
        List<String> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                "<< /Length " + stream.length + " >>\nstream\n" + text + "\nendstream"
        );
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int index = 0; index < objects.size(); index++) {
            offsets.add(pdf.length());
            pdf.append(index + 1).append(" 0 obj\n").append(objects.get(index)).append("\nendobj\n");
        }
        int xref = pdf.length();
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n0000000000 65535 f \n");
        for (int offset : offsets) {
            pdf.append("%010d 00000 n \n".formatted(offset));
        }
        pdf.append("trailer << /Root 1 0 R /Size ").append(objects.size() + 1).append(" >>\nstartxref\n").append(xref).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapePdf(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private UUID recordReport(UUID userProfileId, String type, String format, LocalDate start, LocalDate end, String filename) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into generated_reports (
                    id, user_profile_id, report_type, format, period_start, period_end, status, file_name,
                    metadata_json, generated_at, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp, current_timestamp)
                """,
                id,
                userProfileId,
                type,
                format,
                start,
                end,
                "GENERATED",
                filename,
                writeJson(Map.of("deterministic", true))
        );
        return id;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not write report metadata.", exception);
        }
    }

    public record ExportFile(String filename, String contentType, byte[] bytes) {
    }
}
