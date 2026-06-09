package com.spendsense.api.service.finance;

import com.spendsense.api.dto.finance.AccountResponse;
import com.spendsense.api.exception.ResourceNotFoundException;
import com.spendsense.api.mapper.finance.AccountMapper;
import com.spendsense.api.repository.finance.AccountRepository;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.user.UserProfileSyncService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final UserProfileSyncService userProfileSyncService;

    public AccountService(
            AccountRepository accountRepository,
            AccountMapper accountMapper,
            UserProfileSyncService userProfileSyncService
    ) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
        this.userProfileSyncService = userProfileSyncService;
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
}
