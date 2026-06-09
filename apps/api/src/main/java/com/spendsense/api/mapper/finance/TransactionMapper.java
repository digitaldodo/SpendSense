package com.spendsense.api.mapper.finance;

import com.spendsense.api.domain.finance.Transaction;
import com.spendsense.api.dto.finance.TransactionDetailResponse;
import com.spendsense.api.dto.finance.TransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {
    private final AccountMapper accountMapper;
    private final CategoryMapper categoryMapper;

    public TransactionMapper(AccountMapper accountMapper, CategoryMapper categoryMapper) {
        this.accountMapper = accountMapper;
        this.categoryMapper = categoryMapper;
    }

    public TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getDirection(),
                transaction.getStatus(),
                transaction.getOccurredAt(),
                transaction.getBookedAt(),
                transaction.getMerchantName(),
                transaction.getMerchantNormalized(),
                transaction.getDescription(),
                transaction.getReference(),
                transaction.getSource(),
                accountMapper.toResponse(transaction.getAccount()),
                categoryMapper.toResponse(transaction.getCategory())
        );
    }

    public TransactionDetailResponse toDetailResponse(Transaction transaction) {
        return new TransactionDetailResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getDirection(),
                transaction.getStatus(),
                transaction.getOccurredAt(),
                transaction.getBookedAt(),
                transaction.getMerchantName(),
                transaction.getMerchantNormalized(),
                transaction.getDescription(),
                transaction.getReference(),
                transaction.getSource(),
                transaction.getSourceTransactionId(),
                transaction.getIdempotencyKey(),
                transaction.getDedupeFingerprint(),
                transaction.getIngestionSession() == null ? null : transaction.getIngestionSession().getId(),
                accountMapper.toResponse(transaction.getAccount()),
                categoryMapper.toResponse(transaction.getCategory()),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}
