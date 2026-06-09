package com.spendsense.api.service.finance;

import com.spendsense.api.domain.finance.Transaction;
import com.spendsense.api.domain.finance.TransactionDirection;
import com.spendsense.api.dto.finance.DashboardFinanceSummaryResponse;
import com.spendsense.api.dto.finance.PageResponse;
import com.spendsense.api.dto.finance.TransactionDetailResponse;
import com.spendsense.api.dto.finance.TransactionFilterRequest;
import com.spendsense.api.dto.finance.TransactionResponse;
import com.spendsense.api.exception.ResourceNotFoundException;
import com.spendsense.api.mapper.finance.AccountMapper;
import com.spendsense.api.mapper.finance.TransactionMapper;
import com.spendsense.api.repository.finance.AccountRepository;
import com.spendsense.api.repository.finance.TransactionRepository;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.user.UserProfileSyncService;
import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
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
    private final TransactionMapper transactionMapper;
    private final AccountMapper accountMapper;
    private final UserProfileSyncService userProfileSyncService;
    private final Clock clock;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            TransactionMapper transactionMapper,
            AccountMapper accountMapper,
            UserProfileSyncService userProfileSyncService
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.transactionMapper = transactionMapper;
        this.accountMapper = accountMapper;
        this.userProfileSyncService = userProfileSyncService;
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
        Page<Transaction> recent = transactionRepository.findAll(
                toSpecification(userProfileId, new TransactionFilterRequest(
                        null, null, null, null, null, null, null, 0, 6, "-occurredAt"
                )),
                PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "occurredAt"))
        );

        return new DashboardFinanceSummaryResponse(
                accountRepository.countByUserProfileId(userProfileId),
                transactionRepository.countByUserProfileId(userProfileId),
                transactionRepository.countByUserProfileIdAndSource(
                        userProfileId,
                        com.spendsense.api.domain.finance.IngestionSource.DEMO
                ) > 0,
                accountRepository.sumCurrentBalance(userProfileId),
                transactionRepository.sumAmountForDirectionBetween(
                        userProfileId,
                        TransactionDirection.DEBIT,
                        monthStart,
                        nextMonthStart
                ),
                transactionRepository.sumAmountForDirectionBetween(
                        userProfileId,
                        TransactionDirection.CREDIT,
                        monthStart,
                        nextMonthStart
                ),
                accountRepository.findByUserProfileIdOrderByCreatedAtAsc(userProfileId)
                        .stream()
                        .map(accountMapper::toResponse)
                        .toList(),
                recent.getContent().stream().map(transactionMapper::toResponse).toList()
        );
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
}
