package com.spendsense.api.service.finance;

import com.spendsense.api.config.SpendSenseProperties;
import com.spendsense.api.domain.finance.Account;
import com.spendsense.api.domain.finance.AccountType;
import com.spendsense.api.domain.finance.Category;
import com.spendsense.api.domain.finance.IngestionSession;
import com.spendsense.api.domain.finance.IngestionSource;
import com.spendsense.api.domain.finance.Transaction;
import com.spendsense.api.domain.finance.TransactionDirection;
import com.spendsense.api.domain.finance.TransactionStatus;
import com.spendsense.api.domain.user.UserProfile;
import com.spendsense.api.dto.finance.DemoSeedResponse;
import com.spendsense.api.exception.ResourceNotFoundException;
import com.spendsense.api.repository.finance.AccountRepository;
import com.spendsense.api.repository.finance.CategoryRepository;
import com.spendsense.api.repository.finance.IngestionSessionRepository;
import com.spendsense.api.repository.finance.TransactionRepository;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.user.UserProfileSyncService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoFinanceSeedService {
    private final SpendSenseProperties properties;
    private final UserProfileSyncService userProfileSyncService;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final IngestionSessionRepository ingestionSessionRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    public DemoFinanceSeedService(
            SpendSenseProperties properties,
            UserProfileSyncService userProfileSyncService,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            IngestionSessionRepository ingestionSessionRepository,
            TransactionRepository transactionRepository,
            CategoryService categoryService
    ) {
        this.properties = properties;
        this.userProfileSyncService = userProfileSyncService;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.ingestionSessionRepository = ingestionSessionRepository;
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
    }

    @Transactional
    public DemoSeedResponse seedForCurrentUser(SupabasePrincipal principal) {
        if (!demoEnabled()) {
            throw new ResourceNotFoundException("Demo seeding is not enabled for this environment.");
        }

        UserProfile profile = userProfileSyncService.syncAuthenticatedUser(principal);
        if (transactionRepository.countByUserProfileIdAndSource(profile.getId(), IngestionSource.DEMO) > 0) {
            return new DemoSeedResponse(0, 0, true);
        }

        categoryService.ensureSystemCategories();
        Account hdfc = accountRepository.save(new Account(
                profile,
                "HDFC Salary Account",
                "HDFC Bank",
                AccountType.SAVINGS,
                "4821",
                "INR",
                new BigDecimal("186420.75"),
                IngestionSource.DEMO,
                "demo-hdfc-4821"
        ));
        Account icici = accountRepository.save(new Account(
                profile,
                "ICICI Credit Card",
                "ICICI Bank",
                AccountType.CREDIT_CARD,
                "9910",
                "INR",
                new BigDecimal("-12480.50"),
                IngestionSource.DEMO,
                "demo-icici-9910"
        ));

        IngestionSession session = new IngestionSession(
                profile,
                IngestionSource.DEMO,
                "{\"mode\":\"local-demo\",\"locale\":\"en-IN\"}"
        );
        ingestionSessionRepository.save(session);

        Instant now = Instant.now();
        List<DemoTransactionSeed> seeds = List.of(
                debit("Swiggy Instamart", "Groceries and essentials", "food-dining", "842.00", 1, hdfc),
                debit("Zomato", "Dinner order", "food-dining", "612.50", 1, hdfc),
                debit("Uber India", "Airport ride", "transport", "734.00", 2, icici),
                debit("Amazon Pay India", "Household purchase", "shopping", "2499.00", 3, icici),
                credit("Acme Fintech Pvt Ltd", "Salary credit", "income", "185000.00", 5, hdfc),
                debit("UPI to Rohan Mehta", "Weekend settlement", "transfers", "2200.00", 6, hdfc),
                debit("BESCOM", "Electricity bill", "bills", "1830.00", 7, hdfc),
                debit("Swiggy", "Lunch order", "food-dining", "386.00", 9, icici),
                debit("Apollo Pharmacy", "Medicines", "health", "1285.00", 11, hdfc),
                debit("Zomato", "Coffee and snacks", "food-dining", "418.00", 13, icici),
                debit("Myntra", "Clothing purchase", "shopping", "3199.00", 16, icici),
                credit("UPI from Neha Shah", "Shared cab reimbursement", "transfers", "460.00", 18, hdfc),
                debit("Uber Auto", "Office commute", "transport", "168.00", 20, hdfc),
                debit("Jio Recharge", "Mobile plan", "bills", "799.00", 22, hdfc),
                debit("Amazon Fresh", "Monthly groceries", "food-dining", "4215.00", 24, icici),
                debit("UPI to House Help", "Monthly payment", "transfers", "4500.00", 27, hdfc)
        );

        int index = 0;
        for (DemoTransactionSeed seed : seeds) {
            index++;
            Category category = category(seed.categorySlug());
            Instant occurredAt = now.minus(seed.daysAgo(), ChronoUnit.DAYS);
            String sourceTransactionId = "demo-txn-" + index;
            transactionRepository.save(new Transaction(
                    profile,
                    seed.account(),
                    category,
                    session,
                    new BigDecimal(seed.amount()),
                    "INR",
                    seed.direction(),
                    TransactionStatus.POSTED,
                    occurredAt,
                    occurredAt.plus(4, ChronoUnit.HOURS),
                    seed.merchantName(),
                    normalizeMerchant(seed.merchantName()),
                    seed.description(),
                    "UPI/DEMO/" + (100000 + index),
                    IngestionSource.DEMO,
                    sourceTransactionId,
                    profile.getId() + ":" + sourceTransactionId,
                    fingerprint(profile.getId() + "|" + sourceTransactionId + "|" + seed.amount()),
                    "{\"source\":\"demo\",\"merchant\":\"" + seed.merchantName() + "\"}"
            ));
        }
        session.complete(seeds.size(), seeds.size(), 0);
        ingestionSessionRepository.save(session);

        return new DemoSeedResponse(2, seeds.size(), false);
    }

    private boolean demoEnabled() {
        return properties.demo() != null && Boolean.TRUE.equals(properties.demo().enabled());
    }

    private Category category(String slug) {
        return categoryRepository.findBySlugAndUserProfileIdIsNull(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Demo category not found."));
    }

    private static DemoTransactionSeed debit(
            String merchantName,
            String description,
            String categorySlug,
            String amount,
            int daysAgo,
            Account account
    ) {
        return new DemoTransactionSeed(
                merchantName,
                description,
                categorySlug,
                amount,
                daysAgo,
                account,
                TransactionDirection.DEBIT
        );
    }

    private static DemoTransactionSeed credit(
            String merchantName,
            String description,
            String categorySlug,
            String amount,
            int daysAgo,
            Account account
    ) {
        return new DemoTransactionSeed(
                merchantName,
                description,
                categorySlug,
                amount,
                daysAgo,
                account,
                TransactionDirection.CREDIT
        );
    }

    private static String normalizeMerchant(String merchantName) {
        return merchantName
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private static String fingerprint(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest unavailable.", exception);
        }
    }

    private record DemoTransactionSeed(
            String merchantName,
            String description,
            String categorySlug,
            String amount,
            int daysAgo,
            Account account,
            TransactionDirection direction
    ) {
    }
}
