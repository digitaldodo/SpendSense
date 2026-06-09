package com.spendsense.api.repository.finance;

import com.spendsense.api.domain.finance.Transaction;
import com.spendsense.api.domain.finance.Account;
import com.spendsense.api.domain.finance.TransactionDirection;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {
    @EntityGraph(attributePaths = {"account", "category", "ingestionSession"})
    Optional<Transaction> findByIdAndUserProfileId(UUID id, UUID userProfileId);

    long countByUserProfileId(UUID userProfileId);

    long countByUserProfileIdAndSource(UUID userProfileId, com.spendsense.api.domain.finance.IngestionSource source);

    boolean existsByUserProfileIdAndDedupeFingerprint(UUID userProfileId, String dedupeFingerprint);

    boolean existsByUserProfileIdAndIdempotencyKey(UUID userProfileId, String idempotencyKey);

    List<Transaction> findByIdInAndUserProfileId(List<UUID> ids, UUID userProfileId);

    @Modifying
    @Query("update Transaction t set t.account = :targetAccount where t.userProfile.id = :userProfileId and t.account.id = :sourceAccountId")
    int moveTransactionsToAccount(UUID userProfileId, UUID sourceAccountId, Account targetAccount);

    @Query("""
            select coalesce(sum(t.amount), 0)
            from Transaction t
            where t.userProfile.id = :userProfileId
              and t.direction = :direction
              and t.occurredAt >= :from
              and t.occurredAt < :to
              and t.status <> com.spendsense.api.domain.finance.TransactionStatus.EXCLUDED
            """)
    BigDecimal sumAmountForDirectionBetween(
            UUID userProfileId,
            TransactionDirection direction,
            Instant from,
            Instant to
    );

    @Query(value = """
            select
                cast(c.id as varchar) as categoryId,
                coalesce(c.name, 'Uncategorized') as name,
                coalesce(c.color_token, 'neutral') as colorToken,
                coalesce(sum(t.amount), 0) as total,
                count(*) as transactionCount
            from transactions t
            left join categories c on c.id = t.category_id
            where t.user_profile_id = :userProfileId
              and t.direction = 'DEBIT'
              and t.status <> 'EXCLUDED'
              and t.occurred_at >= :from
              and t.occurred_at < :to
            group by c.id, c.name, c.color_token
            order by total desc
            """, nativeQuery = true)
    List<CategorySpendProjection> categorySpendBetween(UUID userProfileId, Instant from, Instant to);

    @Query(value = """
            select
                date_trunc('month', t.occurred_at) as periodStart,
                coalesce(sum(case when t.direction = 'CREDIT' then t.amount else 0 end), 0) as income,
                coalesce(sum(case when t.direction = 'DEBIT' then t.amount else 0 end), 0) as expense
            from transactions t
            where t.user_profile_id = :userProfileId
              and t.status <> 'EXCLUDED'
              and t.occurred_at >= :from
              and t.occurred_at < :to
            group by date_trunc('month', t.occurred_at)
            order by periodStart asc
            """, nativeQuery = true)
    List<MonthlySummaryProjection> monthlySummaryBetween(UUID userProfileId, Instant from, Instant to);

    interface CategorySpendProjection {
        String getCategoryId();

        String getName();

        String getColorToken();

        BigDecimal getTotal();

        Long getTransactionCount();
    }

    interface MonthlySummaryProjection {
        OffsetDateTime getPeriodStart();

        BigDecimal getIncome();

        BigDecimal getExpense();
    }
}
