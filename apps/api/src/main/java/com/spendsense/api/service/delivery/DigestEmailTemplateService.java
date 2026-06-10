package com.spendsense.api.service.delivery;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class DigestEmailTemplateService {
    private static final Locale INDIA = Locale.forLanguageTag("en-IN");

    public EmailTemplate renderDigest(DigestSnapshot snapshot) {
        String title = switch (snapshot.templateType()) {
            case "WEEKLY_SUMMARY" -> "Your weekly SpendSense summary";
            case "MONTHLY_FINANCIAL_SUMMARY" -> "Your monthly financial summary";
            case "BUDGET_ALERTS" -> "Budget alerts from SpendSense";
            case "RECURRING_PAYMENT_REMINDERS" -> "Upcoming recurring payments";
            default -> "SpendSense delivery update";
        };
        String intro = switch (snapshot.templateType()) {
            case "WEEKLY_SUMMARY" -> "A quiet look at the week that just closed.";
            case "MONTHLY_FINANCIAL_SUMMARY" -> "A deterministic summary of the month, based on your ledger.";
            case "BUDGET_ALERTS" -> "A few budget thresholds are worth reviewing.";
            case "RECURRING_PAYMENT_REMINDERS" -> "These recurring payments are expected soon.";
            default -> "Your latest SpendSense report is ready.";
        };
        String html = """
                <!doctype html>
                <html>
                  <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1" />
                    <style>
                      body { margin:0; background:#f5f7f8; color:#17201b; font-family:Arial, Helvetica, sans-serif; }
                      .wrap { max-width:640px; margin:0 auto; padding:24px 14px; }
                      .panel { background:#ffffff; border:1px solid #dfe7e2; border-radius:8px; overflow:hidden; }
                      .header { padding:24px; border-bottom:1px solid #edf1ee; }
                      .brand { color:#23614b; font-weight:700; font-size:14px; letter-spacing:0; }
                      h1 { margin:10px 0 8px; font-size:24px; line-height:1.25; }
                      p { margin:0; line-height:1.6; }
                      .muted { color:#64736b; font-size:14px; }
                      .grid { display:block; padding:18px 24px 6px; }
                      .metric { border:1px solid #e4ebe7; border-radius:8px; padding:14px; margin-bottom:10px; }
                      .label { color:#64736b; font-size:12px; }
                      .value { margin-top:6px; font-size:20px; font-weight:700; }
                      .section { padding:16px 24px 24px; }
                      .item { padding:12px 0; border-top:1px solid #edf1ee; }
                      .footer { color:#6c7a72; font-size:12px; padding:18px 24px 24px; }
                      @media (max-width: 520px) { h1 { font-size:21px; } .header, .grid, .section, .footer { padding-left:18px; padding-right:18px; } }
                    </style>
                  </head>
                  <body>
                    <div class="wrap">
                      <div class="panel">
                        <div class="header">
                          <div class="brand">SpendSense</div>
                          <h1>%s</h1>
                          <p class="muted">%s</p>
                          <p class="muted">%s to %s</p>
                        </div>
                        <div class="grid">
                          %s
                        </div>
                        <div class="section">
                          %s
                        </div>
                        <div class="footer">Generated deterministically from your SpendSense data. You can change digest and delivery settings anytime.</div>
                      </div>
                    </div>
                  </body>
                </html>
                """.formatted(
                escape(title),
                escape(intro),
                snapshot.periodStart(),
                snapshot.periodEnd(),
                renderMetrics(snapshot),
                renderItems(snapshot)
        );
        String text = """
                SpendSense
                %s
                %s
                Period: %s to %s

                Income: %s
                Spending: %s
                Net cashflow: %s

                %s
                """.formatted(
                title,
                intro,
                snapshot.periodStart(),
                snapshot.periodEnd(),
                money(snapshot.income(), snapshot.currency()),
                money(snapshot.expense(), snapshot.currency()),
                money(snapshot.netCashflow(), snapshot.currency()),
                snapshot.items().isEmpty() ? "No priority items for this delivery." : String.join("\n", snapshot.items())
        );
        return new EmailTemplate(snapshot.templateType(), title, html, text);
    }

    private String renderMetrics(DigestSnapshot snapshot) {
        return """
                <div class="metric"><div class="label">Income</div><div class="value">%s</div></div>
                <div class="metric"><div class="label">Spending</div><div class="value">%s</div></div>
                <div class="metric"><div class="label">Net cashflow</div><div class="value">%s</div></div>
                """.formatted(
                money(snapshot.income(), snapshot.currency()),
                money(snapshot.expense(), snapshot.currency()),
                money(snapshot.netCashflow(), snapshot.currency())
        );
    }

    private String renderItems(DigestSnapshot snapshot) {
        if (snapshot.items().isEmpty()) {
            return "<p class=\"muted\">No priority items for this delivery.</p>";
        }
        StringBuilder builder = new StringBuilder();
        for (String item : snapshot.items()) {
            builder.append("<div class=\"item\"><p>")
                    .append(escape(item))
                    .append("</p></div>");
        }
        return builder.toString();
    }

    private String money(BigDecimal amount, String currency) {
        NumberFormat format = NumberFormat.getCurrencyInstance(INDIA);
        format.setCurrency(java.util.Currency.getInstance(currency == null ? "INR" : currency));
        return format.format(amount == null ? BigDecimal.ZERO : amount);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public record DigestSnapshot(
            String templateType,
            LocalDate periodStart,
            LocalDate periodEnd,
            String currency,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal netCashflow,
            List<String> items
    ) {
    }
}
