package com.spendsense.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.spendsense.api.domain.finance.TransactionDirection;
import com.spendsense.api.domain.finance.Account;
import com.spendsense.api.domain.finance.AccountType;
import com.spendsense.api.domain.finance.Category;
import com.spendsense.api.domain.finance.IngestionSource;
import com.spendsense.api.domain.finance.Transaction;
import com.spendsense.api.domain.finance.TransactionStatus;
import com.spendsense.api.dto.finance.BudgetRequest;
import com.spendsense.api.dto.finance.TransactionFilterRequest;
import com.spendsense.api.repository.finance.AccountRepository;
import com.spendsense.api.repository.finance.CategoryRepository;
import com.spendsense.api.repository.finance.TransactionRepository;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.security.UserRole;
import com.spendsense.api.service.finance.FinancialInsightsService;
import com.spendsense.api.service.finance.AccountService;
import com.spendsense.api.service.finance.DemoFinanceSeedService;
import com.spendsense.api.service.finance.PlanningService;
import com.spendsense.api.service.finance.ReportExportService;
import com.spendsense.api.service.finance.TransactionService;
import com.spendsense.api.service.user.UserProfileSyncService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spendsense.demo.enabled=true")
@ActiveProfiles("test")
class SpendSenseApiApplicationTests {
	private final SupabasePrincipal principal = new SupabasePrincipal(
			UUID.fromString("20000000-0000-4000-8000-000000000001"),
			"demo@spendsense.local",
			Set.of(UserRole.USER),
			Map.of()
	);
	private final SupabasePrincipal insightsPrincipal = new SupabasePrincipal(
			UUID.fromString("20000000-0000-4000-8000-000000000002"),
			"insights@spendsense.local",
			Set.of(UserRole.USER),
			Map.of()
	);

	@Autowired
	private DemoFinanceSeedService demoFinanceSeedService;

	@Autowired
	private FinancialInsightsService financialInsightsService;

	@Autowired
	private ReportExportService reportExportService;

	@Autowired
	private PlanningService planningService;

	@Autowired
	private TransactionService transactionService;

	@Autowired
	private AccountService accountService;

	@Autowired
	private UserProfileSyncService userProfileSyncService;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@BeforeEach
	void seedDemoData() {
		demoFinanceSeedService.seedForCurrentUser(principal);
	}

	@Test
	void contextLoads() {
	}

	@Test
	void demoSeedSupportsDashboardSummary() {
		var summary = transactionService.dashboardSummary(principal);

		assertThat(summary.demoSeeded()).isTrue();
		assertThat(summary.accountCount()).isEqualTo(2);
		assertThat(summary.transactionCount()).isEqualTo(16);
		assertThat(summary.recentTransactions()).hasSize(6);
	}

	@Test
	void transactionPaginationAndFilteringWork() {
		var firstPage = transactionService.listTransactions(principal, new TransactionFilterRequest(
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				0,
				5,
				"-amount"
		));
		var swiggyOnly = transactionService.listTransactions(principal, new TransactionFilterRequest(
				null,
				null,
				null,
				null,
				"swiggy",
				null,
				null,
				0,
				10,
				"-occurredAt"
		));
		var creditsOnly = transactionService.listTransactions(principal, new TransactionFilterRequest(
				null,
				null,
				TransactionDirection.CREDIT,
				null,
				null,
				null,
				null,
				0,
				10,
				"-occurredAt"
		));

		assertThat(firstPage.items()).hasSize(5);
		assertThat(firstPage.totalItems()).isEqualTo(16);
		assertThat(firstPage.hasNext()).isTrue();
		assertThat(swiggyOnly.totalItems()).isEqualTo(2);
		assertThat(creditsOnly.totalItems()).isEqualTo(2);
	}

	@Test
	void accountsApiReturnsSeededAccounts() {
		assertThat(accountService.listAccounts(principal)).hasSize(2);
	}

	@Test
	void deterministicInsightsDetectRecurringPaymentsAndGenerateReports() {
		Category bills = categoryRepository.findBySlugAndUserProfileIdIsNull("bills").orElseThrow();
		Account account = testAccount("insights-detection");
		LocalDate monthStart = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
		createTransaction(account, bills, "StreamBox", "streambox", "899.00", TransactionDirection.DEBIT, monthStart.minusMonths(3).plusDays(4));
		createTransaction(account, bills, "StreamBox", "streambox", "899.00", TransactionDirection.DEBIT, monthStart.minusMonths(2).plusDays(4));
		createTransaction(account, bills, "StreamBox", "streambox", "899.00", TransactionDirection.DEBIT, monthStart.minusMonths(1).plusDays(4));
		createTransaction(account, bills, "StreamBox", "streambox", "899.00", TransactionDirection.DEBIT, monthStart.plusDays(4));
		createTransaction(account, bills, "Acme Payroll", "acme payroll", "100000.00", TransactionDirection.CREDIT, monthStart.plusDays(1));

		var insights = financialInsightsService.insights(insightsPrincipal, monthStart.minusMonths(4), monthStart.plusMonths(1).minusDays(1));
		var csv = reportExportService.csvExport(insightsPrincipal, monthStart.minusMonths(1), monthStart.plusMonths(1).minusDays(1), "monthly-summary");
		var pdf = reportExportService.pdfExport(insightsPrincipal, YearMonth.from(monthStart));
		var report = reportExportService.monthlyReport(insightsPrincipal, YearMonth.from(monthStart));

		assertThat(insights.recurringTransactions()).anyMatch(pattern -> pattern.merchantNormalized().equals("streambox"));
		assertThat(insights.subscriptions()).anyMatch(pattern -> pattern.merchantNormalized().equals("streambox"));
		assertThat(insights.monthlyComparisons()).isNotEmpty();
		assertThat(csv.contentType()).isEqualTo("text/csv");
		assertThat(new String(csv.bytes(), StandardCharsets.UTF_8)).contains("month", "subscriptions", "StreamBox");
		assertThat(pdf.contentType()).isEqualTo("application/pdf");
		assertThat(new String(pdf.bytes(), StandardCharsets.UTF_8)).startsWith("%PDF-1.4");
		assertThat(report.categoryBreakdown()).isNotEmpty();
	}

	@Test
	void budgetRolloversMaterializeFromPostedPriorMonthSpend() {
		Category food = categoryRepository.findBySlugAndUserProfileIdIsNull("food-dining").orElseThrow();
		Account account = testAccount("rollover");
		LocalDate monthStart = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
		createTransaction(account, food, "Grocer", "grocer", "3000.00", TransactionDirection.DEBIT, monthStart.minusMonths(1).plusDays(5));
		planningService.createBudget(insightsPrincipal, new BudgetRequest(
				food.getId(),
				"Food rollover",
				new BigDecimal("10000.00"),
				"INR",
				monthStart,
				true,
				"Test rollover budget"
		));

		var rollovers = financialInsightsService.materializeBudgetRollovers(insightsPrincipal);

		assertThat(rollovers).anySatisfy(rollover -> {
			assertThat(rollover.categoryName()).isEqualTo("Food & Dining");
			assertThat(rollover.spentAmount()).isEqualByComparingTo("3000.00");
			assertThat(rollover.rolloverAmount()).isEqualByComparingTo("7000.00");
		});
	}

	private Account testAccount(String suffix) {
		var profile = userProfileSyncService.syncAuthenticatedUser(insightsPrincipal);
		return accountRepository.save(new Account(
				profile,
				"Insights Test " + suffix,
				"SpendSense Test Bank",
				AccountType.SAVINGS,
				"0002",
				"INR",
				new BigDecimal("100000.00"),
				IngestionSource.MANUAL,
				"test-" + suffix
		));
	}

	private void createTransaction(
			Account account,
			Category category,
			String merchantName,
			String merchantNormalized,
			String amount,
			TransactionDirection direction,
			LocalDate date
	) {
		Instant occurredAt = date.atStartOfDay().toInstant(ZoneOffset.UTC);
		String key = account.getId() + "|" + merchantNormalized + "|" + amount + "|" + date;
		transactionRepository.save(new Transaction(
				account.getUserProfile(),
				account,
				category,
				null,
				new BigDecimal(amount),
				"INR",
				direction,
				TransactionStatus.POSTED,
				occurredAt,
				occurredAt,
				merchantName,
				merchantNormalized,
				"Test transaction",
				"TEST/" + key,
				IngestionSource.MANUAL,
				key,
				key,
				fingerprint(key),
				"{\"source\":\"test\"}"
		));
	}

	private static String fingerprint(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return java.util.HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 digest unavailable.", exception);
		}
	}
}
