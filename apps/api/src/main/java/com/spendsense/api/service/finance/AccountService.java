package com.spendsense.api.service.finance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.domain.finance.Account;
import com.spendsense.api.domain.finance.ImportJobStatus;
import com.spendsense.api.domain.finance.ReconciliationLog;
import com.spendsense.api.domain.user.UserProfile;
import com.spendsense.api.dto.finance.AccountMergeRequest;
import com.spendsense.api.dto.finance.AccountResponse;
import com.spendsense.api.dto.finance.BalanceCorrectionRequest;
import com.spendsense.api.exception.ResourceNotFoundException;
import com.spendsense.api.mapper.finance.AccountMapper;
import com.spendsense.api.repository.finance.AccountRepository;
import com.spendsense.api.repository.finance.ReconciliationLogRepository;
import com.spendsense.api.repository.finance.TransactionRepository;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.user.UserProfileSyncService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ReconciliationLogRepository reconciliationLogRepository;
    private final AccountMapper accountMapper;
    private final UserProfileSyncService userProfileSyncService;
    private final ObjectMapper objectMapper;

    public AccountService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            ReconciliationLogRepository reconciliationLogRepository,
            AccountMapper accountMapper,
            UserProfileSyncService userProfileSyncService,
            ObjectMapper objectMapper
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.reconciliationLogRepository = reconciliationLogRepository;
        this.accountMapper = accountMapper;
        this.userProfileSyncService = userProfileSyncService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<AccountResponse> listAccounts(SupabasePrincipal principal) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        return accountRepository.findByUserProfileIdOrderByCreatedAtAsc(userProfileId)
                .stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    @Transactional
    public AccountResponse getAccount(SupabasePrincipal principal, UUID accountId) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        return accountRepository.findByIdAndUserProfileId(accountId, userProfileId)
                .map(accountMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
    }

    @Transactional
    public AccountResponse mergeAccount(SupabasePrincipal principal, UUID sourceAccountId, AccountMergeRequest request) {
        UserProfile profile = userProfileSyncService.syncAuthenticatedUser(principal);
        if (sourceAccountId.equals(request.targetAccountId())) {
            throw new IllegalArgumentException("Choose two different accounts to merge.");
        }
        Account source = accountRepository.findByIdAndUserProfileId(sourceAccountId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found."));
        Account target = accountRepository.findByIdAndUserProfileId(request.targetAccountId(), profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Target account not found."));
        BigDecimal targetOpening = target.getCurrentBalance();
        int moved = transactionRepository.moveTransactionsToAccount(profile.getId(), source.getId(), target);
        target.applyBalanceDelta(source.getCurrentBalance());
        source.markMerged(writeJson(new AccountMergeMetadata(
                target.getId(),
                moved,
                request.reason(),
                Instant.now()
        )));
        accountRepository.save(source);
        Account savedTarget = accountRepository.save(target);
        reconciliationLogRepository.save(new ReconciliationLog(
                profile,
                null,
                savedTarget,
                ImportJobStatus.COMPLETED,
                moved,
                moved,
                0,
                0,
                targetOpening,
                savedTarget.getCurrentBalance(),
                source.getCurrentBalance(),
                writeJson(new AccountMergeMetadata(source.getId(), moved, request.reason(), Instant.now()))
        ));
        return accountMapper.toResponse(savedTarget);
    }

    @Transactional
    public AccountResponse correctBalance(SupabasePrincipal principal, UUID accountId, BalanceCorrectionRequest request) {
        UserProfile profile = userProfileSyncService.syncAuthenticatedUser(principal);
        Account account = accountRepository.findByIdAndUserProfileId(accountId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        BigDecimal opening = account.getCurrentBalance();
        account.correctBalance(request.correctedBalance(), writeJson(new BalanceCorrectionMetadata(
                opening,
                request.correctedBalance(),
                request.reason(),
                Instant.now()
        )));
        Account saved = accountRepository.save(account);
        reconciliationLogRepository.save(new ReconciliationLog(
                profile,
                null,
                saved,
                ImportJobStatus.COMPLETED,
                0,
                0,
                0,
                0,
                opening,
                saved.getCurrentBalance(),
                saved.getCurrentBalance().subtract(opening),
                writeJson(new BalanceCorrectionMetadata(opening, saved.getCurrentBalance(), request.reason(), Instant.now()))
        ));
        return accountMapper.toResponse(saved);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not write account reconciliation metadata.", exception);
        }
    }

    private record AccountMergeMetadata(UUID relatedAccountId, int movedTransactions, String reason, Instant occurredAt) {
    }

    private record BalanceCorrectionMetadata(BigDecimal previousBalance, BigDecimal correctedBalance, String reason, Instant occurredAt) {
    }
}
