package com.spendsense.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.spendsense.api.domain.finance.TransactionDirection;
import com.spendsense.api.dto.finance.TransactionFilterRequest;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.security.UserRole;
import com.spendsense.api.service.finance.AccountService;
import com.spendsense.api.service.finance.DemoFinanceSeedService;
import com.spendsense.api.service.finance.TransactionService;
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

	@Autowired
	private DemoFinanceSeedService demoFinanceSeedService;

	@Autowired
	private TransactionService transactionService;

	@Autowired
	private AccountService accountService;

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
}
