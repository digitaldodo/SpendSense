package com.spendsense.api.service.delivery;

import com.spendsense.api.service.delivery.DigestEmailTemplateService.DigestSnapshot;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

@Service
public class DigestGenerationService {
    private final JdbcTemplate jdbcTemplate;
    private final DigestEmailTemplateService templateService;
    private final Clock clock;

    public DigestGenerationService(JdbcTemplate jdbcTemplate, DigestEmailTemplateService templateService) {
        this.jdbcTemplate = jdbcTemplate;
        this.templateService = templateService;
        this.clock = Clock.systemUTC();
    }

    public EmailTemplate preview(UUID userProfileId, String templateType) {
        return templateService.renderDigest(snapshot(userProfileId, normalizeTemplate(templateType)));
    }

    public EmailTemplate weeklySummary(UUID userProfileId) {
        return templateService.renderDigest(snapshot(userProfileId, "WEEKLY_SUMMARY"));
    }

    public EmailTemplate monthlySummary(UUID userProfileId) {
        return templateService.renderDigest(snapshot(userProfileId, "MONTHLY_FINANCIAL_SUMMARY"));
    }

    public EmailTemplate budgetAlerts(UUID userProfileId) {
        return templateService.renderDigest(snapshot(userProfileId, "BUDGET_ALERTS"));
    }

    public EmailTemplate recurringPaymentReminders(UUID userProfileId) {
        return templateService.renderDigest(snapshot(userProfileId, "RECURRING_PAYMENT_REMINDERS"));
    }

    private DigestSnapshot snapshot(UUID userProfileId, String templateType) {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = switch (templateType) {
            case "WEEKLY_SUMMARY", "RECURRING_PAYMENT_REMINDERS" -> today.minusDays(7);
            case "BUDGET_ALERTS" -> today.withDayOfMonth(1);
            default -> today.minusMonths(1).withDayOfMonth(1);
        };
        LocalDate end = switch (templateType) {
            case "WEEKLY_SUMMARY", "RECURRING_PAYMENT_REMINDERS" -> today.minusDays(1);
            case "BUDGET_ALERTS" -> today;
            default -> today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        };
        MoneySummary moneySummary = jdbcTemplate.queryForObject("""
                select
                    coalesce(sum(case when direction = 'CREDIT' then amount else 0 end), 0) as income,
                    coalesce(sum(case when direction = 'DEBIT' then amount else 0 end), 0) as expense,
                    coalesce(max(currency), 'INR') as currency
                from transactions
                where user_profile_id = ? and status <> 'EXCLUDED'
                  and occurred_at >= ? and occurred_at < ?
                """,
                this::moneySummaryRow,
                userProfileId,
                start.atStartOfDay().toInstant(ZoneOffset.UTC),
                end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        );
        if (moneySummary == null) {
            moneySummary = new MoneySummary(BigDecimal.ZERO, BigDecimal.ZERO, "INR");
        }
        return new DigestSnapshot(
                templateType,
                start,
                end,
                moneySummary.currency(),
                moneySummary.income(),
                moneySummary.expense(),
                moneySummary.income().subtract(moneySummary.expense()),
                items(userProfileId, templateType, start, end)
        );
    }

    private List<String> items(UUID userProfileId, String templateType, LocalDate start, LocalDate end) {
        List<String> items = new ArrayList<>();
        if (templateType.equals("BUDGET_ALERTS") || templateType.equals("WEEKLY_SUMMARY") || templateType.equals("MONTHLY_FINANCIAL_SUMMARY")) {
            jdbcTemplate.query("""
                    select c.name as category_name, b.amount, b.currency, coalesce(sum(t.amount), 0) as spent
                    from budgets b
                    join categories c on c.id = b.category_id
                    left join transactions t on t.user_profile_id = b.user_profile_id
                        and t.category_id = b.category_id
                        and t.direction = 'DEBIT'
                        and t.status <> 'EXCLUDED'
                        and t.occurred_at >= ?
                        and t.occurred_at < ?
                    where b.user_profile_id = ? and b.active = true
                    group by c.name, b.amount, b.currency
                    having b.amount > 0 and (coalesce(sum(t.amount), 0) / b.amount) >= 0.8
                    order by (coalesce(sum(t.amount), 0) / b.amount) desc
                    limit 5
                    """, (RowCallbackHandler) rs -> items.add("%s is at %d%% of budget.".formatted(
                    rs.getString("category_name"),
                    rs.getBigDecimal("spent")
                            .multiply(BigDecimal.valueOf(100))
                            .divide(rs.getBigDecimal("amount"), 0, java.math.RoundingMode.HALF_UP)
                            .intValue()
            )), start.atStartOfDay().toInstant(ZoneOffset.UTC), end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC), userProfileId);
        }
        if (templateType.equals("RECURRING_PAYMENT_REMINDERS") || templateType.equals("WEEKLY_SUMMARY")) {
            jdbcTemplate.query("""
                    select merchant_name, amount, currency, next_expected_on
                    from recurring_transactions
                    where user_profile_id = ? and state = 'ACTIVE'
                      and next_expected_on between ? and ?
                    order by next_expected_on asc, amount desc
                    limit 5
                    """, (RowCallbackHandler) rs -> items.add("%s is expected on %s for %s %s.".formatted(
                    rs.getString("merchant_name"),
                    rs.getObject("next_expected_on", LocalDate.class),
                    rs.getString("currency"),
                    rs.getBigDecimal("amount")
            )), userProfileId, LocalDate.now(clock), LocalDate.now(clock).plusDays(7));
        }
        if (items.isEmpty() && templateType.equals("MONTHLY_FINANCIAL_SUMMARY")) {
            items.add("No budget or recurring payment alerts were generated for this period.");
        }
        return items;
    }

    private MoneySummary moneySummaryRow(ResultSet rs, int rowNum) throws SQLException {
        return new MoneySummary(rs.getBigDecimal("income"), rs.getBigDecimal("expense"), rs.getString("currency"));
    }

    private String normalizeTemplate(String templateType) {
        String normalized = templateType == null ? "WEEKLY_SUMMARY" : templateType.trim().toUpperCase().replace("-", "_");
        return switch (normalized) {
            case "MONTHLY", "MONTHLY_SUMMARY", "MONTHLY_FINANCIAL_SUMMARY" -> "MONTHLY_FINANCIAL_SUMMARY";
            case "BUDGET_ALERTS" -> "BUDGET_ALERTS";
            case "RECURRING_PAYMENT_REMINDERS" -> "RECURRING_PAYMENT_REMINDERS";
            default -> "WEEKLY_SUMMARY";
        };
    }

    private record MoneySummary(BigDecimal income, BigDecimal expense, String currency) {
    }
}
