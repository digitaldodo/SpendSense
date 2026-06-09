package com.spendsense.api.service.finance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.domain.finance.Category;
import com.spendsense.api.domain.finance.Transaction;
import com.spendsense.api.domain.finance.TransactionDirection;
import com.spendsense.api.domain.finance.TransactionEdit;
import com.spendsense.api.domain.finance.TransactionStatus;
import com.spendsense.api.domain.user.UserProfile;
import com.spendsense.api.dto.finance.BulkTransactionActionRequest;
import com.spendsense.api.dto.finance.BulkTransactionActionResponse;
import com.spendsense.api.dto.finance.CategorySpendResponse;
import com.spendsense.api.dto.finance.DashboardFinanceSummaryResponse;
import com.spendsense.api.dto.finance.MonthlySummaryResponse;
import com.spendsense.api.dto.finance.PageResponse;
import com.spendsense.api.dto.finance.TransactionDetailResponse;
import com.spendsense.api.dto.finance.TransactionFilterRequest;
import com.spendsense.api.dto.finance.TransactionResponse;
import com.spendsense.api.dto.finance.TransactionUpdateRequest;
import com.spendsense.api.exception.ResourceNotFoundException;
import com.spendsense.api.mapper.finance.AccountMapper;
import com.spendsense.api.mapper.finance.ImportMapper;
import com.spendsense.api.mapper.finance.TransactionMapper;
import com.spendsense.api.repository.finance.AccountRepository;
import com.spendsense.api.repository.finance.CategoryRepository;
import com.spendsense.api.repository.finance.ImportJobRepository;
import com.spendsense.api.repository.finance.TransactionEditRepository;
import com.spendsense.api.repository.finance.TransactionRepository;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.user.UserProfileSyncService;
import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final ImportJobRepository importJobRepository;
    private final TransactionEditRepository transactionEditRepository;
    private final TransactionMapper transactionMapper;
    private final AccountMapper accountMapper;
    private final ImportMapper importMapper;
    private final UserProfileSyncService userProfileSyncService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            ImportJobRepository importJobRepository,
            TransactionEditRepository transactionEditRepository,
            TransactionMapper transactionMapper,
            AccountMapper accountMapper,
            ImportMapper importMapper,
            UserProfileSyncService userProfileSyncService,
            ObjectMapper objectMapper
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.importJobRepository = importJobRepository;
        this.transactionEditRepository = transactionEditRepository;
        this.transactionMapper = transactionMapper;
        this.accountMapper = accountMapper;
        this.importMapper = importMapper;
        this.userProfileSyncService = userProfileSyncService;
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public PageResponse<TransactionResponse> listTransactions(
            SupabasePrincipal principal,
            TransactionFilterRequest request
    ) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        PageRequest pageRequest = PageRequest.of(
                request.page() == null ? 0 : request.page(),
                request.size() == null ? DEFAULT_PAGE_SIZE : request.size(),
                resolveSort(request.sort())
        );
        Page<Transaction> page = transactionRepository.findAll(toSpecification(userProfileId, request), pageRequest);
        return new PageResponse<>(
                page.getContent().stream().map(transactionMapper::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    @Transactional
    public TransactionDetailResponse getTransaction(SupabasePrincipal principal, UUID transactionId) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        return transactionRepository.findByIdAndUserProfileId(transactionId, userProfileId)
                .map(transactionMapper::toDetailResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found."));
    }

    @Transactional
    public DashboardFinanceSummaryResponse dashboardSummary(SupabasePrincipal principal) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        Instant monthStart = LocalDate.now(clock)
                .withDayOfMonth(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
        Instant nextMonthStart = LocalDate.now(clock)
                .withDayOfMonth(1)
                .plusMonths(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
        Instant sixMonthsStart = LocalDate.now(clock)
                .withDayOfMonth(1)
                .minusMonths(5)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
        Page<Transaction> recent = transactionRepository.findAll(
                toSpecification(userProfileId, new TransactionFilterRequest(
                        null, null, null, null, null, null, null, 0, 6, "-occurredAt"
                )),
                PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "occurredAt"))
        );
        BigDecimal monthSpend = transactionRepository.sumAmountForDirectionBetween(
                userProfileId,
                TransactionDirection.DEBIT,
                monthStart,
                nextMonthStart
        );
        BigDecimal monthIncome = transactionRepository.sumAmountForDirectionBetween(
                userProfileId,
                TransactionDirection.CREDIT,
                monthStart,
                nextMonthStart
        );
        var categoryRows = transactionRepository.categorySpendBetween(userProfileId, monthStart, nextMonthStart);
        BigDecimal categoryTotal = categoryRows.stream()
                .map(TransactionRepository.CategorySpendProjection::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardFinanceSummaryResponse(
                accountRepository.countByUserProfileId(userProfileId),
                transactionRepository.countByUserProfileId(userProfileId),
                transactionRepository.countByUserProfileIdAndSource(
                        userProfileId,
                        com.spendsense.api.domain.finance.IngestionSource.DEMO
                ) > 0,
                accountRepository.sumCurrentBalance(userProfileId),
                monthSpend,
                monthIncome,
                monthIncome.subtract(monthSpend),
                accountRepository.findByUserProfileIdOrderByCreatedAtAsc(userProfileId)
                        .stream()
                        .map(accountMapper::toResponse)
                        .toList(),
                recent.getContent().stream().map(transactionMapper::toResponse).toList(),
                categoryRows.stream()
                        .map(row -> new CategorySpendResponse(
                                row.getCategoryId() == null ? null : UUID.fromString(row.getCategoryId()),
                                row.getName(),
                                row.getColorToken(),
                                row.getTotal(),
                                row.getTransactionCount() == null ? 0 : row.getTransactionCount(),
                                categoryTotal.signum() == 0
                                        ? BigDecimal.ZERO
                                        : row.getTotal().multiply(BigDecimal.valueOf(100)).divide(categoryTotal, 2, RoundingMode.HALF_UP)
                        ))
                        .toList(),
                transactionRepository.monthlySummaryBetween(userProfileId, sixMonthsStart, nextMonthStart)
                        .stream()
                        .map(row -> new MonthlySummaryResponse(
                                row.getPeriodStart().toInstant(),
                                row.getIncome(),
                                row.getExpense(),
                                row.getIncome().subtract(row.getExpense())
                        ))
                        .toList(),
                importJobRepository.findTop20ByUserProfileIdOrderByStartedAtDesc(userProfileId)
                        .stream()
                        .limit(5)
                        .map(importMapper::toResponse)
                        .toList()
        );
    }

    @Transactional
    public TransactionDetailResponse updateTransaction(
            SupabasePrincipal principal,
            UUID transactionId,
            TransactionUpdateRequest request
    ) {
        UserProfile profile = userProfileSyncService.syncAuthenticatedUser(principal);
        Transaction transaction = transactionRepository.findByIdAndUserProfileId(transactionId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found."));
        Category category = request.categoryId() == null
                ? transaction.getCategory()
                : categoryRepository.findVisibleById(request.categoryId(), profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));
        TransactionStatus status = request.status() == null ? transaction.getStatus() : request.status();
        String beforeJson = writeJson(new TransactionEditSnapshot(
                transaction.getCategory() == null ? null : transaction.getCategory().getId(),
                transaction.getStatus()
        ));
        transaction.updateCategory(category);
        transaction.updateStatus(status);
        Transaction saved = transactionRepository.save(transaction);
        transactionEditRepository.save(new TransactionEdit(
                profile,
                saved,
                "TRANSACTION_UPDATE",
                beforeJson,
                writeJson(new TransactionEditSnapshot(category == null ? null : category.getId(), status)),
                request.reason()
        ));
        return transactionMapper.toDetailResponse(saved);
    }

    @Transactional
    public BulkTransactionActionResponse bulkUpdateTransactions(
            SupabasePrincipal principal,
            BulkTransactionActionRequest request
    ) {
        UserProfile profile = userProfileSyncService.syncAuthenticatedUser(principal);
        Category category = request.categoryId() == null
                ? null
                : categoryRepository.findVisibleById(request.categoryId(), profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));
        var transactions = transactionRepository.findByIdInAndUserProfileId(request.transactionIds(), profile.getId());
        for (Transaction transaction : transactions) {
            String beforeJson = writeJson(new TransactionEditSnapshot(
                    transaction.getCategory() == null ? null : transaction.getCategory().getId(),
                    transaction.getStatus()
            ));
            if (request.categoryId() != null) {
                transaction.updateCategory(category);
            }
            if (request.status() != null) {
                transaction.updateStatus(request.status());
            }
            transactionEditRepository.save(new TransactionEdit(
                    profile,
                    transaction,
                    "BULK_TRANSACTION_UPDATE",
                    beforeJson,
                    writeJson(new TransactionEditSnapshot(
                            transaction.getCategory() == null ? null : transaction.getCategory().getId(),
                            transaction.getStatus()
                    )),
                    request.reason()
            ));
        }
        transactionRepository.saveAll(transactions);
        return new BulkTransactionActionResponse(request.transactionIds().size(), transactions.size());
    }

    private Specification<Transaction> toSpecification(UUID userProfileId, TransactionFilterRequest request) {
        return (root, query, criteriaBuilder) -> {
            if (query.getResultType() != Long.class) {
                root.fetch("account", JoinType.LEFT);
                root.fetch("category", JoinType.LEFT);
            }
            var predicate = criteriaBuilder.equal(root.get("userProfile").get("id"), userProfileId);

            if (request.accountId() != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("account").get("id"), request.accountId()));
            }
            if (request.categoryId() != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("category").get("id"), request.categoryId()));
            }
            if (request.direction() != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("direction"), request.direction()));
            }
            if (request.status() != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), request.status()));
            }
            if (request.from() != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.greaterThanOrEqualTo(root.get("occurredAt"), request.from()));
            }
            if (request.to() != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.lessThan(root.get("occurredAt"), request.to()));
            }
            if (request.search() != null && !request.search().isBlank()) {
                String term = "%" + request.search().trim().toLowerCase(Locale.ROOT) + "%";
                var accountJoin = root.join("account", JoinType.LEFT);
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.or(
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("merchantName")), term),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("merchantNormalized")), term),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), term),
                                criteriaBuilder.like(criteriaBuilder.lower(accountJoin.get("displayName")), term)
                        )
                );
            }
            return predicate;
        };
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "occurredAt");
        }
        String normalized = sort.trim();
        Sort.Direction direction = normalized.startsWith("-") ? Sort.Direction.DESC : Sort.Direction.ASC;
        String property = normalized.startsWith("-") ? normalized.substring(1) : normalized;
        property = switch (property) {
            case "amount" -> "amount";
            case "merchant" -> "merchantNormalized";
            case "createdAt" -> "createdAt";
            default -> "occurredAt";
        };
        return Sort.by(direction, property);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not write transaction audit metadata.", exception);
        }
    }

    private record TransactionEditSnapshot(UUID categoryId, TransactionStatus status) {
    }
}
