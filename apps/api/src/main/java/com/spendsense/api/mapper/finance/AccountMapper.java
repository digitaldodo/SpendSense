package com.spendsense.api.mapper.finance;

import com.spendsense.api.domain.finance.Account;
import com.spendsense.api.dto.finance.AccountResponse;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {
    public AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getDisplayName(),
                account.getInstitutionName(),
                account.getAccountType(),
                account.getAccountMask(),
                account.getCurrency(),
                account.getCurrentBalance(),
                account.getAvailableBalance(),
                account.getStatus(),
                account.getSource(),
                account.getConnectedAt(),
                account.getLastSyncedAt()
        );
    }
}
